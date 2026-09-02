-- ============================================================
-- V46: 상점/경매(임시 관리자)/룰렛 방어권/유저간 송금 제거
--
--   남기는 것: 룰렛, 파칭코, 슬롯, 출석체크, 포인트 원장, 그리고 ADMIN/ROOT 전용 유저 정지.
--   제거하는 것: 상점 카탈로그, 유료 활동 차단권, ADMIN 권한 경매, 룰렛 방어권,
--              유저간 송금, 포인트로 정지를 사고 푸는 모든 경로.
--
--   포인트/거래 데이터는 전부 초기화한다(운영 합의). 그래서 과거 이력 보존을 위해
--   enum 값을 남겨둘 필요가 없어, CHECK 제약에서도 제거 대상 타입을 완전히 지운다.
--
--   실행 순서가 중요하다 — 1~3단계는 곧 드롭할 테이블을 "읽어서" 유저 상태를 정리하므로
--   반드시 5단계(DROP)보다 먼저 와야 한다. 순서를 바꾸면 조용히 아래가 남는다:
--     · 경매로 받은 ADMIN 권한이 회수 주체를 잃고 영구히 남음 (권한 유출)
--     · 방어권 보유자의 룰렛 동의가 꺼진 채 영구 고정됨
-- ============================================================

-- ------------------------------------------------------------
-- 1. 경매로 임시 부여된 ADMIN 권한 회수
--    평소엔 RoleAuctionScheduler.expireRolePurchases()가 기한이 되면 USER로 되돌린다.
--    스케줄러와 테이블을 같이 지우므로, 지금 살아있는 부여분을 여기서 직접 회수한다.
--    ROOT가 수동 지정한 ADMIN은 role_purchases에 행이 없으므로 영향받지 않는다.
-- ------------------------------------------------------------
UPDATE users u
SET role = 'USER'
WHERE u.role = 'ADMIN'
  AND EXISTS (SELECT 1
              FROM role_purchases rp
              WHERE rp.user_id = u.id
                AND rp.status = 'ACTIVE');

-- ------------------------------------------------------------
-- 2. 룰렛 방어권 보유자의 룰렛 동의 상태 원복
--    방어권 구매 시 roulette_status를 false로 내리고 구매 직전 값을 스냅샷해둔 뒤,
--    만료 시 RouletteShieldService.expireShields()가 되돌리는 구조였다.
--    테이블을 지우면 되돌릴 근거가 사라지므로 지금 원복한다.
-- ------------------------------------------------------------
UPDATE users u
SET roulette_status = true
WHERE EXISTS (SELECT 1
              FROM roulette_shields rs
              WHERE rs.user_id = u.id
                AND rs.status = 'ACTIVE'
                AND rs.previous_roulette_status = true);

-- ------------------------------------------------------------
-- 3. 진행 중인 제재 전면 해제
--    ADMIN 정지가 activity_blocks로 대체되어 있었고 그 테이블을 드롭하므로, 정지는
--    users.status/suspended_until 한 경로로 통일된다. 기존 제재는 근거 데이터가
--    사라지는 만큼 전부 풀고 시작한다(필요하면 관리자가 다시 걸면 됨).
-- ------------------------------------------------------------
UPDATE users
SET status          = 'ACTIVE',
    suspended_until = NULL
WHERE status = 'SUSPENDED';

-- ------------------------------------------------------------
-- 4. 포인트 데이터 초기화
--    transactions는 append-only 원장이라 일부만 지우면 prev/post 잔액 스냅샷이
--    실제 balance와 어긋난다. 전부 비우고 잔액도 0으로 맞춰 정합성을 유지한다.
--    points의 행 자체(유저별 계좌 + user_id IS NULL인 시스템 계정)는 남긴다.
-- ------------------------------------------------------------
DELETE FROM transactions;

UPDATE points
SET balance    = 0,
    updated_at = now();

-- 고정 룰렛 상대 설정도 게임 데이터이므로 같이 비운다.
DELETE FROM roulette_opponents;

-- ------------------------------------------------------------
-- 5. 제거 대상 테이블 드롭 (FK 의존 역순)
--    block_appeals → activity_blocks → shop_items,
--    role_bids → role_auction_rounds → shop_items 순으로 참조가 걸려 있다.
-- ------------------------------------------------------------
DROP TABLE IF EXISTS block_appeals;
DROP TABLE IF EXISTS activity_blocks;
DROP TABLE IF EXISTS roulette_shields;
DROP TABLE IF EXISTS role_bids;
DROP TABLE IF EXISTS role_purchases;
DROP TABLE IF EXISTS role_auction_rounds;
DROP TABLE IF EXISTS shop_items;
DROP TABLE IF EXISTS suspend_unlock_settings;
DROP TABLE IF EXISTS suspend_duration_presets;

-- ------------------------------------------------------------
-- 6. transactions CHECK 제약 재작성
--    제거 타입: TRANSFER, TRANSFER_FEE, BLOCK_PURCHASE, BLOCK_UNBLOCK,
--              BID_HOLD, BID_REFUND, SUSPEND_UNLOCK, SHIELD_PURCHASE, JMT
--    (JMT는 V38의 TODO에 있던 임시 타입 — 이번에 같이 정리한다.)
--    4단계에서 행을 모두 비웠으므로 제약을 좁혀도 걸릴 기존 데이터가 없다.
-- ------------------------------------------------------------
ALTER TABLE transactions DROP CONSTRAINT ck_transactions_type;
ALTER TABLE transactions
    ADD CONSTRAINT ck_transactions_type
        CHECK (type IN ('GRANT', 'COLLECT', 'ACCOUNT_OPENED', 'CHECKIN',
                        'ROULETTE', 'ROULETTE_FEE',
                        'PACHINKO', 'PACHINKO_FEE', 'SLOT', 'SLOT_FEE'));

-- amount 버킷도 같이 좁힌다. TRANSFER_FEE가 빠지면서 "amount >= 0" 그룹은
-- CHECKIN/ROULETTE/PACHINKO/SLOT 넷만 남는다.
ALTER TABLE transactions DROP CONSTRAINT ck_transactions_amount;
ALTER TABLE transactions
    ADD CONSTRAINT ck_transactions_amount CHECK (
        (type = 'ACCOUNT_OPENED' AND amount = 0) OR
        (type IN ('CHECKIN', 'ROULETTE', 'PACHINKO', 'SLOT') AND amount >= 0) OR
        (type NOT IN ('ACCOUNT_OPENED', 'CHECKIN', 'ROULETTE', 'PACHINKO', 'SLOT') AND amount > 0)
        );

-- ------------------------------------------------------------
-- 7. notifications 정리 (V45와 동일한 패턴 — 행 삭제 후 제약 재작성)
--    댓글/좋아요 등 커뮤니티 알림은 그대로 두고, 제거 기능의 알림만 지운다.
-- ------------------------------------------------------------
DELETE FROM notifications WHERE type IN ('TRANSFER', 'ROLE_AUCTION_WON', 'ROLE_AUCTION_LOST');

ALTER TABLE notifications DROP CONSTRAINT ck_notifications_type;
ALTER TABLE notifications
    ADD CONSTRAINT ck_notifications_type
        CHECK (type IN ('COMMENT', 'REPLY', 'LIKE', 'CONTENT_REMOVED',
                        'POINT_STOLEN', 'POINT_WON',
                        'POST_REWARD', 'COMMENT_REWARD'));

-- "게시글과 무관한 알림(post_id IS NULL)" 목록에도 제거 대상 타입이 박혀 있다.
-- 여기까지 갱신해야 제거된 타입 이름이 스키마에 하나도 안 남는다.
ALTER TABLE notifications DROP CONSTRAINT ck_notifications_post_id_by_type;
ALTER TABLE notifications
    ADD CONSTRAINT ck_notifications_post_id_by_type CHECK (
        (type IN ('POINT_STOLEN', 'POINT_WON') AND post_id IS NULL) OR
        (type NOT IN ('POINT_STOLEN', 'POINT_WON') AND post_id IS NOT NULL)
        );
