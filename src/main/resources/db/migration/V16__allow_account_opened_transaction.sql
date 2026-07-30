-- ============================================================
-- V16: transactions에 ACCOUNT_OPENED(계좌 개설 감사 로그) 타입 허용
--   금액 이동이 없는 감사 로그이므로 amount=0을 허용하고,
--   상대 계좌(시스템 계정)를 건드리지 않으므로 sender 측 잔액 컬럼을 NULL 허용으로 변경한다.
-- ============================================================

ALTER TABLE transactions
DROP CONSTRAINT ck_transactions_type;
ALTER TABLE transactions
    ADD CONSTRAINT ck_transactions_type CHECK (type IN ('GRANT', 'COLLECT', 'ACCOUNT_OPENED'));

ALTER TABLE transactions
DROP CONSTRAINT transactions_amount_check;
ALTER TABLE transactions
    ADD CONSTRAINT ck_transactions_amount CHECK (
        (type = 'ACCOUNT_OPENED' AND amount = 0) OR
        (type <> 'ACCOUNT_OPENED' AND amount > 0)
        );

ALTER TABLE transactions
    ALTER COLUMN prev_sender_balance DROP NOT NULL;
ALTER TABLE transactions
    ALTER COLUMN post_sender_balance DROP NOT NULL;
