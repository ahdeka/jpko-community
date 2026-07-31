-- ============================================================
-- V21: notifications에 POINT_WON(룰렛 대결 승리로 포인트를 얻음) 타입 허용
--   POINT_STOLEN과 마찬가지로 게시글과 무관해 post_id가 없어야 한다.
-- ============================================================

ALTER TABLE notifications
DROP CONSTRAINT ck_notifications_type;
ALTER TABLE notifications
    ADD CONSTRAINT ck_notifications_type
        CHECK (type IN ('COMMENT', 'REPLY', 'LIKE', 'CONTENT_REMOVED', 'POINT_STOLEN', 'POINT_WON'));

ALTER TABLE notifications
DROP CONSTRAINT ck_notifications_post_id_by_type;
ALTER TABLE notifications
    ADD CONSTRAINT ck_notifications_post_id_by_type
        CHECK (
            (type IN ('POINT_STOLEN', 'POINT_WON') AND post_id IS NULL) OR
            (type NOT IN ('POINT_STOLEN', 'POINT_WON') AND post_id IS NOT NULL)
            );
