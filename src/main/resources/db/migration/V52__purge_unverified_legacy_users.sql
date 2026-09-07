-- ============================================================
-- V52: 이메일 인증 없이 가입했던 레거시/더미 계정과 그 활동 데이터 전면 삭제
--
--   가입 절차가 "가입 전 이메일 인증(PRE_SIGNUP) 필수"로 바뀌면서, login_id가 없거나
--   email_verified=false인 계정은 더 이상 정상 경로로 만들어질 수 없다. V22가 남겨둔
--   "기존 유저는 login_id NULL, 추후 온보딩으로 채우게 유도" 전환 기간을 여기서 끝낸다.
--   ROOT/ADMIN은 운영 계정이라 조건에 걸려도 보호한다.
-- ============================================================

-- ------------------------------------------------------------
-- 0. 삭제 대상 유저
-- ------------------------------------------------------------
CREATE TEMP TABLE doomed_users ON COMMIT DROP AS
SELECT id
FROM users
WHERE (login_id IS NULL OR email_verified = false)
  AND role NOT IN ('ROOT', 'ADMIN');

-- ------------------------------------------------------------
-- 0-1. 방어: 대상 유저가 공지를 작성했다면 중단
--      공지는 관리자 전용 기능이라 여기 걸릴 일이 없어야 정상이다. 걸린다면 공지를
--      조용히 지우지 말고 사람이 작성자를 이관한 뒤 다시 실행해야 한다.
-- ------------------------------------------------------------
DO
$$
    DECLARE
cnt bigint;
BEGIN
SELECT count(*) INTO cnt
FROM notices
WHERE user_id IN (SELECT id FROM doomed_users);

IF cnt > 0 THEN
            RAISE EXCEPTION '삭제 대상 유저가 작성한 공지가 %건 있습니다. 작성자를 먼저 이관하세요.', cnt;
END IF;
END
$$;

-- ------------------------------------------------------------
-- 1. 삭제될 게시글
-- ------------------------------------------------------------
CREATE TEMP TABLE doomed_posts ON COMMIT DROP AS
SELECT id
FROM posts
WHERE user_id IN (SELECT id FROM doomed_users);

-- ------------------------------------------------------------
-- 2. 삭제될 댓글
--    (a) 대상 유저가 쓴 댓글
--    (b) 삭제될 게시글에 달린 모든 댓글 — 글이 사라지므로 남는 유저가 쓴 것도 포함된다
--    (c) (a)(b)에 달린 대댓글 서브트리 전체 — fk_comments_parent 때문에 빠뜨리면 FK 위반
--    soft delete(deleted_at)된 댓글도 행은 남아 있으므로 자연히 포함된다.
-- ------------------------------------------------------------
CREATE TEMP TABLE doomed_comments ON COMMIT DROP AS
WITH RECURSIVE seed AS (SELECT id
                        FROM comments
                        WHERE user_id IN (SELECT id FROM doomed_users)
                        UNION
                        SELECT id
                        FROM comments
                        WHERE target_type = 'POST'
                          AND target_id IN (SELECT id FROM doomed_posts)),
               tree AS (SELECT id
                        FROM seed
                        UNION
                        SELECT c.id
                        FROM comments c
                                 JOIN tree t ON c.parent_id = t.id)
SELECT id
FROM tree;

-- ------------------------------------------------------------
-- 3. 알림 — receiver/sender/post/comment 어느 쪽으로든 걸리면 삭제
-- ------------------------------------------------------------
DELETE
FROM notifications
WHERE receiver_id IN (SELECT id FROM doomed_users)
   OR sender_id IN (SELECT id FROM doomed_users)
   OR post_id IN (SELECT id FROM doomed_posts)
   OR comment_id IN (SELECT id FROM doomed_comments);

-- ------------------------------------------------------------
-- 4. 신고 — target_id는 FK 없는 폴리모픽이라 타입별로 직접 매칭한다
-- ------------------------------------------------------------
DELETE
FROM reports
WHERE reporter_id IN (SELECT id FROM doomed_users)
   OR (target_type = 'POST' AND target_id IN (SELECT id FROM doomed_posts))
   OR (target_type = 'COMMENT' AND target_id IN (SELECT id FROM doomed_comments))
   OR (target_type = 'USER' AND target_id IN (SELECT id FROM doomed_users));

-- ------------------------------------------------------------
-- 5. 좋아요
-- ------------------------------------------------------------
DELETE
FROM likes
WHERE user_id IN (SELECT id FROM doomed_users)
   OR post_id IN (SELECT id FROM doomed_posts);

-- ------------------------------------------------------------
-- 6~7. 댓글 → 게시글
--      부모/자식을 한 DELETE 문에 담으면 FK 트리거가 문장 종료 시점에 검사하므로
--      순서를 더 쪼갤 필요가 없다(2단계에서 서브트리를 전부 모아둔 덕분).
-- ------------------------------------------------------------
DELETE
FROM comments
WHERE id IN (SELECT id FROM doomed_comments);

DELETE
FROM posts
WHERE id IN (SELECT id FROM doomed_posts);

-- ------------------------------------------------------------
-- 8. 포인트 원장 초기화 (V46과 동일한 판단)
--    transactions는 append-only 원장이라 일부만 지우면 prev/post 잔액 스냅샷이 실제
--    balance와 어긋난다. 룰렛처럼 유저↔유저로 오간 거래가 있어서 대상 유저 것만 지우면
--    살아남는 유저의 원장이 깨진다. 전부 비우고 잔액도 0으로 맞춰 정합성을 유지한다.
--    points의 행 자체(유저별 계좌 + user_id IS NULL인 시스템 계정)는 남긴다.
-- ------------------------------------------------------------
DELETE
FROM transactions;

DELETE
FROM points
WHERE user_id IN (SELECT id FROM doomed_users);

UPDATE points
SET balance    = 0,
    updated_at = now();

-- 고정 룰렛 상대 — 양쪽 컬럼 모두 확인해야 한다
DELETE
FROM roulette_opponents
WHERE user_id IN (SELECT id FROM doomed_users)
   OR opponent_id IN (SELECT id FROM doomed_users);

-- ------------------------------------------------------------
-- 9. 레벨 / 토큰
--    refresh_tokens와 verification_tokens는 users에 FK가 없어 자동으로 정리되지
--    않는다. 명시적으로 지우지 않으면 고아 행으로 영구히 남는다.
-- ------------------------------------------------------------
DELETE
FROM user_levels
WHERE user_id IN (SELECT id FROM doomed_users);

DELETE
FROM refresh_tokens
WHERE user_id IN (SELECT id FROM doomed_users);

DELETE
FROM verification_tokens
WHERE user_id IN (SELECT id FROM doomed_users);

-- ------------------------------------------------------------
-- 10. 유저
-- ------------------------------------------------------------
DELETE
FROM users
WHERE id IN (SELECT id FROM doomed_users);

-- ------------------------------------------------------------
-- 11. EMAIL_VERIFICATION 토큰 타입 제거
--     가입 시점에 인증이 끝나므로 "가입 후 이메일 인증" 경로가 사라졌다. 이 타입은
--     더 이상 발급되지 않으니 남은 행을 비우고 CHECK 제약에서도 이름을 지운다.
--     (V22가 만든 두 제약 모두 이 타입을 참조하고 있다.)
-- ------------------------------------------------------------
DELETE
FROM verification_tokens
WHERE type = 'EMAIL_VERIFICATION';

ALTER TABLE verification_tokens
DROP CONSTRAINT ck_verification_tokens_type;
ALTER TABLE verification_tokens
    ADD CONSTRAINT ck_verification_tokens_type
        CHECK (type IN ('PASSWORD_RESET', 'PRE_SIGNUP', 'EMAIL_CHANGE'));

ALTER TABLE verification_tokens
DROP CONSTRAINT ck_verification_tokens_email_fields;
ALTER TABLE verification_tokens
    ADD CONSTRAINT ck_verification_tokens_email_fields CHECK (
        (type = 'PRE_SIGNUP' AND user_id IS NULL AND email IS NOT NULL) OR
        (type = 'EMAIL_CHANGE' AND user_id IS NOT NULL AND email IS NOT NULL) OR
        (type = 'PASSWORD_RESET' AND user_id IS NOT NULL AND email IS NULL)
        );
