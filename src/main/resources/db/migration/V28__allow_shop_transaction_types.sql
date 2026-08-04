-- ============================================================
-- V28: transactions에 상점(shop) 관련 타입 허용
--   BLOCK_PURCHASE/BLOCK_UNBLOCK: 활동 차단권 구매/조기 해제, 항상 amount > 0
--   BID_HOLD/BID_REFUND: 권한 경매 입찰 예탁/환불, 항상 amount > 0
-- ============================================================

ALTER TABLE transactions DROP CONSTRAINT ck_transactions_type;
ALTER TABLE transactions
    ADD CONSTRAINT ck_transactions_type
        CHECK (type IN ('GRANT', 'COLLECT', 'ACCOUNT_OPENED', 'CHECKIN',
                        'ROULETTE', 'ROULETTE_FEE', 'TRANSFER', 'TRANSFER_FEE',
                        'BLOCK_PURCHASE', 'BLOCK_UNBLOCK', 'BID_HOLD', 'BID_REFUND'));

-- amount 제약은 그대로 유지 — 신규 타입들은 기존 "그 외 타입은 amount > 0" 규칙에 자동으로 포함됨
