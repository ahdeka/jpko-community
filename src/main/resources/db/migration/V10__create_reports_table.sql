-- ============================================================
-- V10: 신고(reports) 테이블 생성
--   target_type + target_id 폴리모픽 구조로 설계 (지금 버전은 Post, Comment만 신고 가능)
--   추후 신고 대상이 늘어나도 대응 가능
--   target_type enum 값만 추가하면 확장 가능
--   target_id는 FK 제약을 걸지 않음(폴리모픽이라 단일 컬럼이 여러 테이블을 가리킴)
--   대신 서비스단에서 신고 생성 시점에 대상 존재 여부를 검증
-- ============================================================
CREATE TABLE reports
(
    id          BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT       NOT NULL REFERENCES users (id),
    target_type VARCHAR(20)  NOT NULL,
    target_id   BIGINT       NOT NULL,
    reason      VARCHAR(20)  NOT NULL,
    detail      TEXT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP(6) NOT NULL,
    updated_at  TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_reports_reporter_target UNIQUE (reporter_id, target_type, target_id),
    CONSTRAINT ck_reports_target_type CHECK (target_type IN ('POST', 'COMMENT')),
    CONSTRAINT ck_reports_reason CHECK (reason IN ('SPAM', 'ABUSE', 'ILLEGAL', 'ETC')),
    CONSTRAINT ck_reports_status CHECK (status IN ('PENDING', 'RESOLVED', 'REJECTED'))
);

-- 관리자 집계 조회: "이 대상에 신고가 몇 건 쌓였는지" + PENDING 필터가 주 조회 패턴
CREATE INDEX idx_reports_target ON reports (target_type, target_id, status);

-- 마이페이지 "내 신고 내역" 조회 패턴 (최신순)
CREATE INDEX idx_reports_reporter ON reports (reporter_id, created_at DESC);