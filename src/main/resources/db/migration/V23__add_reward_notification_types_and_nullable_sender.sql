-- ============================================================
-- V22: 시스템이 지급하는 포인트 알림(POST_REWARD/COMMENT_REWARD/LIKE_REWARD) 지원
--   - 시스템 지급 알림은 sender가 없어 sender_id를 nullable로 변경한다.
--   - POST_REWARD(게시글 작성), COMMENT_REWARD(댓글 작성), LIKE_REWARD(좋아요 받음) 타입을 허용한다.
--   세 타입 모두 활동이 벌어진 게시글의 post_id를 채워 보내므로
--   ck_notifications_post_id_by_type 제약은 그대로 둔다.
-- ============================================================

ALTER TABLE notifications
    ALTER COLUMN sender_id DROP NOT NULL;

ALTER TABLE notifications
DROP CONSTRAINT ck_notifications_type;
ALTER TABLE notifications
    ADD CONSTRAINT ck_notifications_type
        CHECK (type IN ('COMMENT', 'REPLY', 'LIKE', 'CONTENT_REMOVED', 'POINT_STOLEN', 'POINT_WON', 'POST_REWARD', 'COMMENT_REWARD', 'LIKE_REWARD'));
