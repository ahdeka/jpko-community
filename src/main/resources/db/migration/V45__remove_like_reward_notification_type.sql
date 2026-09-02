-- ============================================================
-- V45: 좋아요 수신 포인트 보상 기능 제거에 따른 LIKE_REWARD 알림 타입 제거
--   1. 남아있는 LIKE_REWARD 알림 행을 먼저 지움
--   2. CHECK 제약에서도 LIKE_REWARD를 뺌
-- ============================================================

DELETE FROM notifications WHERE type = 'LIKE_REWARD';

ALTER TABLE notifications DROP CONSTRAINT ck_notifications_type;
ALTER TABLE notifications
    ADD CONSTRAINT ck_notifications_type
        CHECK (type IN ('COMMENT', 'REPLY', 'LIKE', 'CONTENT_REMOVED',
                        'POINT_STOLEN', 'POINT_WON', 'TRANSFER',
                        'POST_REWARD', 'COMMENT_REWARD',
                        'ROLE_AUCTION_WON', 'ROLE_AUCTION_LOST'));
