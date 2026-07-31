-- 기존 "일일 로그인 룰렛"(제로섬, 다른 유저를 상대로 강탈)은 "출첵"(시스템 상대,
-- 마이너스~플러스 랜덤 증감)으로 개념이 바뀐다. type/amount 제약을 모두 먼저 넓혀야 아래 UPDATE가
-- 통과한다 — 특히 amount 제약을 UPDATE보다 뒤에 두면, amount=0인 기존 ROULETTE 행(상대 잔액 0으로
-- "0P 당첨"한 이력)이 type만 CHECKIN으로 바뀐 시점에 "아직 CHECKIN을 모르는" 구 제약에 걸려
-- 마이그레이션 자체가 실패한다(구 제약은 ACCOUNT_OPENED/ROULETTE 외 타입은 amount>0을 요구).
ALTER TABLE transactions
DROP CONSTRAINT ck_transactions_type;
ALTER TABLE transactions
    ADD CONSTRAINT ck_transactions_type
        CHECK (type IN ('GRANT', 'COLLECT', 'ACCOUNT_OPENED', 'CHECKIN', 'ROULETTE', 'ROULETTE_FEE'));

ALTER TABLE transactions
DROP CONSTRAINT ck_transactions_amount;
ALTER TABLE transactions
    ADD CONSTRAINT ck_transactions_amount CHECK (
        (type = 'ACCOUNT_OPENED' AND amount = 0) OR
        (type IN ('CHECKIN', 'ROULETTE') AND amount >= 0) OR
        (type NOT IN ('ACCOUNT_OPENED', 'CHECKIN', 'ROULETTE') AND amount > 0)
        );

-- 기존 이력은 CHECKIN으로 이름만 바꾼다. ROULETTE라는 이름은 신규 베팅형 대결 기능이 새로 쓴다.
UPDATE transactions SET type = 'CHECKIN' WHERE type = 'ROULETTE';

-- "오늘 이미 출첵했는지" 조회는 로그인마다 실행되는 hot path이므로 전용 부분 인덱스를 CHECKIN 기준으로 옮긴다.
DROP INDEX idx_transactions_roulette_recipient_created;
CREATE INDEX idx_transactions_checkin_recipient_created
    ON transactions (recipient_id, created_at)
    WHERE type = 'CHECKIN';

-- 유저가 상대를 고정해두면 다음 스핀부터 랜덤 대신 그 상대와 계속 겨룰 수 있다(유저당 최대 1명).
CREATE TABLE roulette_opponents (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    opponent_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
