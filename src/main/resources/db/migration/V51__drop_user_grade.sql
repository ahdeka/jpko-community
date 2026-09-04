-- ============================================================
-- V51: 유저 등급(grade) 컬럼 제거
--    exp 기반 레벨 시스템으로 대체
-- ============================================================

ALTER TABLE users DROP CONSTRAINT ck_users_grade;
ALTER TABLE users DROP COLUMN grade;
