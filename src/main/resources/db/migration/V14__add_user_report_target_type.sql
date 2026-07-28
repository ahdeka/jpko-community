-- ============================================================
-- V14: reports 테이블 신고 대상 타입 USER 컬럼 추가
--   유저(계정) 신고 기능, 이유는 게시글과 그대로 재사용
-- ============================================================
ALTER TABLE reports DROP CONSTRAINT ck_reports_target_type;
ALTER TABLE reports ADD CONSTRAINT ck_reports_target_type
    CHECK (target_type IN ('POST', 'COMMENT', 'USER'));
