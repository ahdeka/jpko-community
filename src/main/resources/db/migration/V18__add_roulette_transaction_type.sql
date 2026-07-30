-- ============================================================
-- V18: transactions에 ROULETTE(일일 포인트 룰렛) 타입 허용
--   유저 -> 유저 이체이며, 피해자 잔액 부족 시 0원까지 뺏을 수 있어 amount=0을 허용한다.
-- ============================================================

ALTER TABLE transactions
DROP CONSTRAINT ck_transactions_type;
ALTER TABLE transactions
    ADD CONSTRAINT ck_transactions_type CHECK (type IN ('GRANT', 'COLLECT', 'ACCOUNT_OPENED', 'ROULETTE'));

ALTER TABLE transactions
DROP CONSTRAINT ck_transactions_amount;
ALTER TABLE transactions
    ADD CONSTRAINT ck_transactions_amount CHECK (
        (type = 'ACCOUNT_OPENED' AND amount = 0) OR
        (type = 'ROULETTE' AND amount >= 0) OR
        (type NOT IN ('ACCOUNT_OPENED', 'ROULETTE') AND amount > 0)
        );

-- "오늘 이미 룰렛을 돌렸는지" 조회는 로그인마다 실행되는 hot path이므로 전용 부분 인덱스로 최적화
CREATE INDEX idx_transactions_roulette_recipient_created
    ON transactions (recipient_id, created_at)
    WHERE type = 'ROULETTE';
