-- ============================================================
-- V50: 기존 유저들 레벨 채우기
--    V50 이전에 가입한 유저 전원에게 exp 0인 행을 만들어 줌
-- ============================================================

INSERT INTO user_levels (user_id, exp, exp_earned_today, exp_earned_date)
SELECT u.id, 0, 0, NULL
FROM users u
    ON CONFLICT (user_id) DO NOTHING;
