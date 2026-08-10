-- ============================================================
-- V39: 포인트 랭킹 인덱스 추가
--   정렬을 한 인덱스로 커버 (balance DESC, user_id ASC) + keyset + top-N
--   시스템 계정은 랭킹에서 제외 (user_id IS NOT NULL)
-- ============================================================

CREATE INDEX idx_points_ranking
    ON points (balance DESC, user_id ASC)
    WHERE user_id IS NOT NULL;
