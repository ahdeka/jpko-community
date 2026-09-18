-- ============================================================
-- V54: 기존 데이터 중 탈퇴한 계정의 login_id / password 파기
-- ============================================================

UPDATE users
SET login_id = 'del-' || id,
    password = NULL
WHERE status = 'DELETED'
  AND (login_id IS DISTINCT FROM 'del-' || id OR password IS NOT NULL);
