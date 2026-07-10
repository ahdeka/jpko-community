-- ============================================================
-- V6: 회원 탈퇴 시각 컬럼 추가
--   deleted_at: 탈퇴 처리 시각 (status=DELETED로 바뀌는 시점에 함께 기록)
--   탈퇴 전에는 NULL. 탈퇴 시 User.withdraw()가 email/nickname/password도
--   함께 익명화하므로, 개인정보 자체는 이 컬럼과 별개로 즉시 파기된다.
-- ============================================================

ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP(6);