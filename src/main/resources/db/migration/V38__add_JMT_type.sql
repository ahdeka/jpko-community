-- ============================================================
-- V34: transactions에 SUSPEND_UNLOCK 타입 허용
--   관리자 정지(SUSPENDED)를 유저가 직접 포인트로 즉시 해제할 때 기록되는 타입. 항상 amount > 0.
--   (V24가 기존 값을 빠뜨렸던 실수를 반복하지 않도록, 매번 전체 목록을 그대로 옮겨 적는다.)
-- ============================================================

ALTER TABLE transactions DROP CONSTRAINT ck_transactions_type;
ALTER TABLE transactions
    ADD CONSTRAINT ck_transactions_type
        CHECK (type IN ('GRANT', 'COLLECT', 'ACCOUNT_OPENED', 'CHECKIN',
                        'ROULETTE', 'ROULETTE_FEE', 'TRANSFER', 'TRANSFER_FEE',
                        'BLOCK_PURCHASE', 'BLOCK_UNBLOCK', 'BID_HOLD', 'BID_REFUND',
                        'SUSPEND_UNLOCK', 'JMT'));

-- 룰렛 참여 동의 여부. 동의한 유저만 랜덤/고정 상대로 선정되거나 직접 스핀할 수 있다.
-- ALTER TABLE users ADD COLUMN roulette_status BOOLEAN NOT NULL DEFAULT false;
