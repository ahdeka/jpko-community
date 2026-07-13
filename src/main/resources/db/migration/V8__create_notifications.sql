-- ============================================================
-- V8: 알림(notifications) 테이블 생성
--   댓글/대댓글/좋아요 발생 시 대상 유저에게 보내는 알림
--   SSE로 실시간 push하되, 오프라인 유저를 위해 DB에도 반드시 저장
-- ============================================================
CREATE TABLE notifications
(
    id          BIGSERIAL PRIMARY KEY,
    receiver_id BIGINT      NOT NULL REFERENCES users (id),
    sender_id   BIGINT      NOT NULL REFERENCES users (id),
    type        VARCHAR(20) NOT NULL,
    post_id     BIGINT      NOT NULL REFERENCES posts (id),
    comment_id  BIGINT REFERENCES comments (id),
    is_read     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP   NOT NULL,
    CONSTRAINT ck_notifications_type
        CHECK (type IN ('COMMENT', 'REPLY', 'LIKE'))
);

-- 조회 패턴: "내 안 읽은 알림 목록" 이 압도적으로 많이 조회됨 → 복합 인덱스
CREATE INDEX idx_notifications_receiver_unread
    ON notifications (receiver_id, is_read, created_at DESC);