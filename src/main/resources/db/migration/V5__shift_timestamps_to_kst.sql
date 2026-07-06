-- ============================================================
-- V5: 기존 timestamp 데이터를 UTC → KST(Asia/Seoul, +9h)로 일괄 보정
--
-- 전제:
--   이 DB의 기존 행은 "전부 UTC로 기록되었다"는 것이 참일 때만 올바르다.
--   운영 DB는 계속 UTC 컨테이너에서 돌았으므로 이 전제가 성립한다.
--   (KST로 이미 기록된 DB에 적용하면 9시간 초과 보정되니 주의)
--
--   대상 컬럼은 모두 TIMESTAMP(6) (timezone 없는 timestamp)이며,
--   NULL 컬럼(deleted_at)은 NULL + interval = NULL 이라 그대로 유지된다.
-- ============================================================

UPDATE categories
SET created_at = created_at + INTERVAL '9 hours',
    updated_at = updated_at + INTERVAL '9 hours';

UPDATE users
SET created_at        = created_at + INTERVAL '9 hours',
    updated_at        = updated_at + INTERVAL '9 hours',
    terms_agreed_at   = terms_agreed_at + INTERVAL '9 hours',
    privacy_agreed_at = privacy_agreed_at + INTERVAL '9 hours';

UPDATE posts
SET created_at = created_at + INTERVAL '9 hours',
    updated_at = updated_at + INTERVAL '9 hours',
    deleted_at = deleted_at + INTERVAL '9 hours';

UPDATE comments
SET created_at = created_at + INTERVAL '9 hours',
    updated_at = updated_at + INTERVAL '9 hours',
    deleted_at = deleted_at + INTERVAL '9 hours';

UPDATE likes
SET created_at = created_at + INTERVAL '9 hours';

UPDATE notices
SET created_at = created_at + INTERVAL '9 hours',
    updated_at = updated_at + INTERVAL '9 hours';

UPDATE refresh_tokens
SET created_at = created_at + INTERVAL '9 hours',
    expires_at = expires_at + INTERVAL '9 hours';

UPDATE verification_tokens
SET created_at = created_at + INTERVAL '9 hours',
    expires_at = expires_at + INTERVAL '9 hours';
