-- ============================================================
-- V30: 두 가지를 한 번에 반영한다.
--   1) 버그 수정: V24가 ck_notifications_type을 다시 쓰면서 V23이 추가했던
--      POST_REWARD/COMMENT_REWARD/LIKE_REWARD를 실수로 빠뜨렸다. 그 이후 이 세 타입의
--      알림 INSERT는 전부 CHECK violation으로 조용히 실패하고 있었다(예외를 로그만
--      남기고 삼키는 리스너 구조라 아무도 몰랐음). 여기서 복구한다.
--   2) 신규: 포인트 기반 ADMIN 권한 경매 낙찰/낙첨 알림(ROLE_AUCTION_WON/ROLE_AUCTION_LOST).
--      POINT_STOLEN/POINT_WON/TRANSFER와 동일하게 게시글과 무관해 post_id가 없어야 한다.
-- ============================================================

ALTER TABLE notifications DROP CONSTRAINT ck_notifications_type;
ALTER TABLE notifications
    ADD CONSTRAINT ck_notifications_type
        CHECK (type IN ('COMMENT', 'REPLY', 'LIKE', 'CONTENT_REMOVED',
                        'POINT_STOLEN', 'POINT_WON', 'TRANSFER',
                        'POST_REWARD', 'COMMENT_REWARD', 'LIKE_REWARD',
                        'ROLE_AUCTION_WON', 'ROLE_AUCTION_LOST'));

ALTER TABLE notifications DROP CONSTRAINT ck_notifications_post_id_by_type;
ALTER TABLE notifications
    ADD CONSTRAINT ck_notifications_post_id_by_type
        CHECK (
            (type IN ('POINT_STOLEN', 'POINT_WON', 'TRANSFER', 'ROLE_AUCTION_WON', 'ROLE_AUCTION_LOST')
                AND post_id IS NULL) OR
            (type NOT IN ('POINT_STOLEN', 'POINT_WON', 'TRANSFER', 'ROLE_AUCTION_WON', 'ROLE_AUCTION_LOST')
                AND post_id IS NOT NULL)
            );
