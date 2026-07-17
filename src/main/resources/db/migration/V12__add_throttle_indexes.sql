-- ============================================================
-- V12: 도배 방지 스로틀링용 인덱스
--   "유저의 마지막 작성 시각" 조회가 글/댓글 작성마다 매번 발생하므로
--   (user_id, created_at DESC) 복합 인덱스로 커버
-- ============================================================
CREATE INDEX idx_posts_user_id_created_at
    ON posts (user_id, created_at DESC);

CREATE INDEX idx_comments_user_id_created_at
    ON comments (user_id, created_at DESC);