-- ============================================================
-- V29: 정지 기한 컬럼 추가
--   경매로 획득한 ADMIN 권한은 기한이 있는 정지만 가능하도록 강제하기 위함.
--   기존 ADMIN/ROOT는 지금처럼 NULL(무기한) 정지를 그대로 사용한다.
-- ============================================================

ALTER TABLE users
    ADD COLUMN suspended_until TIMESTAMP(6);

CREATE INDEX idx_users_status_suspended_until ON users (status, suspended_until) WHERE status = 'SUSPENDED';
