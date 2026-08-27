-- ============================================================
-- V44: 활동 포인트 보상 일일 상한 계산
--   게시글/댓글/좋아요 마다 실행되므로 부분 인덱스로 오늘치만 좁혀서 읽게 하도록
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_transactions_recipient_type_created
    ON transactions (recipient_id, type, created_at)
    WHERE type = 'GRANT';
