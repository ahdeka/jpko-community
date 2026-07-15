-- ============================================================
-- V11: notifications.type CHECK 제약에 CONTENT_REMOVED 허용 추가
--   관리자가 신고/정책위반으로 게시글·댓글을 강제 삭제했을 때
--   작성자에게 "삭제됐다"는 사실만 알리기 위한 알림 타입
-- ============================================================
ALTER TABLE notifications DROP CONSTRAINT ck_notifications_type;
ALTER TABLE notifications ADD CONSTRAINT ck_notifications_type
    CHECK (type IN ('COMMENT', 'REPLY', 'LIKE', 'CONTENT_REMOVED'));