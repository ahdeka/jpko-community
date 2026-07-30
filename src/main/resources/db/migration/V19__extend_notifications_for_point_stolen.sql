-- ============================================================
-- V19: notifications에 POINT_STOLEN(일일 포인트 룰렛으로 포인트를 뺏김) 타입 허용
--   게시글과 무관한 알림이라 post_id를 nullable로 변경하고, 금액을 담을 amount 컬럼을 추가한다.
-- ============================================================

ALTER TABLE notifications
    ALTER COLUMN post_id DROP NOT NULL;

ALTER TABLE notifications
    ADD COLUMN amount BIGINT;

ALTER TABLE notifications
DROP CONSTRAINT ck_notifications_type;
ALTER TABLE notifications
    ADD CONSTRAINT ck_notifications_type
        CHECK (type IN ('COMMENT', 'REPLY', 'LIKE', 'CONTENT_REMOVED', 'POINT_STOLEN'));

-- POINT_STOLEN은 post_id가 없어야 하고, 그 외 타입은 post_id가 필수임을 DB 레벨에서도 보장
ALTER TABLE notifications
    ADD CONSTRAINT ck_notifications_post_id_by_type
        CHECK (
            (type = 'POINT_STOLEN' AND post_id IS NULL) OR
            (type <> 'POINT_STOLEN' AND post_id IS NOT NULL)
            );
