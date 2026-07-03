-- ============================================================
-- V3: 회원가입 약관 동의 시각 컬럼 추가
--   terms_agreed_at   : 이용약관 동의 시각
--   privacy_agreed_at : 개인정보처리방침 동의 시각
-- 회원가입 시점에 동의가 이뤄지므로 두 값은 가입 시각과 동일하게 기록된다.
-- ============================================================

-- 1단계: nullable로 추가 (기존 row가 있어도 실패하지 않도록)
ALTER TABLE users ADD COLUMN terms_agreed_at   TIMESTAMP(6);
ALTER TABLE users ADD COLUMN privacy_agreed_at TIMESTAMP(6);

-- 2단계: 기존 row 백필 (가입 시각 created_at으로 소급)
UPDATE users
SET terms_agreed_at   = created_at,
    privacy_agreed_at = created_at
WHERE terms_agreed_at IS NULL
   OR privacy_agreed_at IS NULL;

-- 3단계: NOT NULL 전환 (이후 모든 신규 가입은 값이 반드시 채워짐)
ALTER TABLE users ALTER COLUMN terms_agreed_at   SET NOT NULL;
ALTER TABLE users ALTER COLUMN privacy_agreed_at SET NOT NULL;