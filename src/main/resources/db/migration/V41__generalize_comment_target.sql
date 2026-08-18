-- ============================================================
-- V41: 댓글 대상을 게시글 전용에서 다형성 있게 확장 (게시글 / 공지)
--   기존 post_id FK에서 target_type과 target_id로 변경
--   댓글이 어떤 대상에 달렸는지를 표현 (POST / NOTICE)
--   기존 댓글은 모두 게시글 댓글이었으니 target_type = 'POST', target_id = post_id로 초기화
-- ============================================================

-- 1. 다형 대상 컬럼 추가 (백필 전이라 우선 nullable)
ALTER TABLE comments
    ADD COLUMN target_type VARCHAR(20);
ALTER TABLE comments
    ADD COLUMN target_id BIGINT;

-- 2. 기존 데이터 백필: 모든 댓글은 게시글 댓글
UPDATE comments
SET target_type = 'POST',
    target_id   = post_id;

-- 3. 백필 완료 후 NOT NULL 확정
ALTER TABLE comments
    ALTER COLUMN target_type SET NOT NULL;
ALTER TABLE comments
    ALTER COLUMN target_id SET NOT NULL;

-- 4. 허용 값 제약 (다른 테이블의 ck_*_type 규칙과 동일한 방식)
ALTER TABLE comments
    ADD CONSTRAINT ck_comments_target_type
        CHECK (target_type IN ('POST', 'NOTICE'));

-- 5. 대상별 댓글 조회용 복합 인덱스 (post_id 참조 조회를 대체)
CREATE INDEX idx_comments_target
    ON comments (target_type, target_id);

-- 6. 게시글 전용 FK/컬럼
--   공지는 FK로 참조하지 않으므로 post_id FK 자체를 걷어낸다.
ALTER TABLE comments
DROP
CONSTRAINT fk_comments_post;
ALTER TABLE comments
DROP
COLUMN post_id;