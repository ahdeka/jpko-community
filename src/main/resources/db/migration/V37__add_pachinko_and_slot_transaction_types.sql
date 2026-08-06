-- ============================================================
-- V37: transactions에 파칭코(PACHINKO/PACHINKO_FEE)와 슬롯(SLOT/SLOT_FEE) 타입 허용
--   PACHINKO_FEE/SLOT_FEE: 유저 -> 시스템, 판당 수수료. 항상 amount > 0 (fee=0이면 아예 기록하지 않음).
--   PACHINKO/SLOT: 유저 <-> 시스템, 한 판의 순손익 정산. 방향은 승/패에 따라 달라지고,
--     정확히 본전(배당률 1.0)인 경우 amount=0도 허용해야 하므로 CHECKIN/ROULETTE/TRANSFER_FEE와
--     같은 "amount >= 0" 버킷에 넣는다.
--   (V24가 기존 값을 빠뜨렸던 실수를 반복하지 않도록, 매번 전체 목록을 그대로 옮겨 적는다.)
-- ============================================================

ALTER TABLE transactions DROP CONSTRAINT ck_transactions_type;
ALTER TABLE transactions
    ADD CONSTRAINT ck_transactions_type
        CHECK (type IN ('GRANT', 'COLLECT', 'ACCOUNT_OPENED', 'CHECKIN',
                        'ROULETTE', 'ROULETTE_FEE', 'TRANSFER', 'TRANSFER_FEE',
                        'BLOCK_PURCHASE', 'BLOCK_UNBLOCK', 'BID_HOLD', 'BID_REFUND',
                        'SUSPEND_UNLOCK', 'PACHINKO_FEE', 'PACHINKO', 'SLOT_FEE', 'SLOT'));

ALTER TABLE transactions DROP CONSTRAINT ck_transactions_amount;
ALTER TABLE transactions
    ADD CONSTRAINT ck_transactions_amount CHECK (
        (type = 'ACCOUNT_OPENED' AND amount = 0) OR
        (type IN ('CHECKIN', 'ROULETTE', 'TRANSFER_FEE', 'PACHINKO', 'SLOT') AND amount >= 0) OR
        (type NOT IN ('ACCOUNT_OPENED', 'CHECKIN', 'ROULETTE', 'TRANSFER_FEE', 'PACHINKO', 'SLOT') AND amount > 0)
        );
