-- ============================================================
-- V22: 로그인 아이디 컬럼 추가 + 가입 전 이메일 인증 지원 + 이메일 변경 지원
--   login_id:
--             로그인 식별자를 email에서 login_id로 분리
--             기존 유저는 NULL로 설정, 추후 업데이트 되도록 유도 (TODO 일정 기간 지나면 refactor 필요)
--   verification_tokens:
--             기존 토큰 발급은 user_id를 기준으로 발급되었으나
--             가입 전 이메일 인증을 위해 email 컬럼 추가
--             PRE_SIGNUP - 가입 전 인증
--             EMAIL_CHANGE - 이메일 변경 및 등록
--             그 외 - 기존대로 user_id 기준
-- ============================================================
ALTER TABLE users ADD COLUMN login_id VARCHAR(20);
ALTER TABLE users ADD CONSTRAINT uk_users_login_id UNIQUE (login_id);

ALTER TABLE verification_tokens ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE verification_tokens ADD COLUMN email VARCHAR(100);

ALTER TABLE verification_tokens DROP CONSTRAINT ck_verification_tokens_type;
ALTER TABLE verification_tokens
    ADD CONSTRAINT ck_verification_tokens_type
        CHECK (type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET',
                        'PRE_SIGNUP', 'EMAIL_CHANGE'));

ALTER TABLE verification_tokens
    ADD CONSTRAINT ck_verification_tokens_email_fields CHECK (
        (type = 'PRE_SIGNUP'   AND user_id IS NULL     AND email IS NOT
            NULL) OR
        (type = 'EMAIL_CHANGE' AND user_id IS NOT NULL AND email IS NOT
            NULL) OR
        (type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET') AND user_id IS NOT
            NULL AND email IS NULL)
        );
