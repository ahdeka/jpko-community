-- ============================================================
-- V15: User 테이블 role 컬럼에 ROOT 타입추가
--   ROOT / ADMIN 권한 분리
-- ============================================================
ALTER TABLE users DROP CONSTRAINT IF EXISTS ck_users_role;
ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN', 'ROOT'));
