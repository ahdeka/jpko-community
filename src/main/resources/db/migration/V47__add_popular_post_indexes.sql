-- ============================================================
-- V47: 인기글 점수 집계용 인덱스 추가
-- ============================================================

-- 인기글은 항상 "삭제되지 않은 최근 N일 글"만 본다 -> 부분 인덱스로 크기를 줄인다
CREATE INDEX idx_posts_created_at_active
    ON posts (created_at DESC)
    WHERE deleted_at IS NULL;
-- 좋아요 집계는 type='LIKE'만 세는데, 기존 uk_likes_post_user(post_id, user_id)는
-- type을 담고 있지 않아 인덱스만으로 개수를 못 센다(테이블을 다시 읽어야 함)
CREATE INDEX idx_likes_post_id_type
    ON likes (post_id, type);
