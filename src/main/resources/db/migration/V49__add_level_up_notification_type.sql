-- ============================================================
-- V49: 레벨업 알림 타입 추가
--    LEVEL_UP은 게시글 타입하고는 무관해서 상관없음
-- ============================================================

ALTER TABLE notifications DROP CONSTRAINT ck_notifications_type;
ALTER TABLE notifications
    ADD CONSTRAINT ck_notifications_type
        CHECK (type IN ('COMMENT', 'REPLY', 'LIKE', 'CONTENT_REMOVED',
                        'POINT_STOLEN', 'POINT_WON',
                        'POST_REWARD', 'COMMENT_REWARD', 'LEVEL_UP'));

ALTER TABLE notifications DROP CONSTRAINT ck_notifications_post_id_by_type;
ALTER TABLE notifications
    ADD CONSTRAINT ck_notifications_post_id_by_type CHECK (
        (type IN ('POINT_STOLEN', 'POINT_WON', 'LEVEL_UP') AND post_id IS NULL) OR
        (type NOT IN ('POINT_STOLEN', 'POINT_WON', 'LEVEL_UP') AND post_id IS NOT NULL)
        );
