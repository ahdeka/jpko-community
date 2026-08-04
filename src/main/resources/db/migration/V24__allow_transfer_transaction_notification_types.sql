-- ============================================================
-- V24: 유저 간 포인트 송금(TRANSFER)과 그 수수료(TRANSFER_FEE) 타입 허용
--   수수료는 시스템이 회수하므로 recipient_id가 없다.
--   수수료는 랜덤이라 0이 될 수 있으므로 amount >= 0 을 허용한다.
-- ============================================================

ALTER TABLE transactions DROP CONSTRAINT ck_transactions_type;
ALTER TABLE transactions
    ADD CONSTRAINT ck_transactions_type
        CHECK (type IN ('GRANT', 'COLLECT', 'ACCOUNT_OPENED', 'CHECKIN',
                        'ROULETTE', 'ROULETTE_FEE', 'TRANSFER', 'TRANSFER_FEE'));

ALTER TABLE transactions DROP CONSTRAINT ck_transactions_amount;
ALTER TABLE transactions
    ADD CONSTRAINT ck_transactions_amount CHECK (
        (type = 'ACCOUNT_OPENED' AND amount = 0) OR
        (type IN ('CHECKIN', 'ROULETTE', 'TRANSFER_FEE') AND amount >= 0) OR
        (type NOT IN ('ACCOUNT_OPENED', 'CHECKIN', 'ROULETTE', 'TRANSFER_FEE') AND amount > 0)
        );
-- ============================================================
-- notifications: TRANSFER(포인트 송금 수신 알림) 타입 허용
--   게시글과 무관하므로 post_id가 없어야 한다.
-- ============================================================

ALTER TABLE notifications DROP CONSTRAINT ck_notifications_type;
ALTER TABLE notifications
    ADD CONSTRAINT ck_notifications_type
        CHECK (type IN ('COMMENT', 'REPLY', 'LIKE', 'CONTENT_REMOVED',
                        'POINT_STOLEN', 'POINT_WON', 'TRANSFER'));

ALTER TABLE notifications DROP CONSTRAINT ck_notifications_post_id_by_type;
ALTER TABLE notifications
    ADD CONSTRAINT ck_notifications_post_id_by_type
        CHECK (
            (type IN ('POINT_STOLEN', 'POINT_WON', 'TRANSFER') AND post_id IS NULL) OR
            (type NOT IN ('POINT_STOLEN', 'POINT_WON', 'TRANSFER') AND post_id IS NOT NULL)
            );