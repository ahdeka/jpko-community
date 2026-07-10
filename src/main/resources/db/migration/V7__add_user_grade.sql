-- ============================================================
-- V7: 유저 등급(grade) 컬럼 추가
--   관리자가 수동으로 부여하는 뱃지성 등급 (권한 role과 무관, 별개 축)
--   사무라이 계급 테마
--   상수 DEFAULT라 기존 row에도 즉시 채워짐 (별도 backfill 불필요)
-- ============================================================
ALTER TABLE users ADD COLUMN grade VARCHAR(20) NOT NULL DEFAULT 'ASHIGARU';
ALTER TABLE users ADD CONSTRAINT ck_users_grade
    CHECK (grade IN ('ASHIGARU', 'SAMURAI', 'HATAMOTO', 'DAIMYO', 'SHOGUN'));