-- ============================================================
-- V50: 기존 유저들 레벨(누적 exp) 채우기
--   과거 활동 이력(글/댓글)을 현행 지급 규칙 그대로 재생해서 exp를 부여해줌
-- ============================================================

WITH activity AS (
    -- 글 1건 = 50exp. 조건 없이 전부 집계한다.
    SELECT p.user_id AS user_id,
           p.created_at::date    AS activity_date, 50 AS exp
    FROM posts p

    UNION ALL

    SELECT c.user_id AS user_id,
           c.created_at::date    AS activity_date, 10 AS exp
    FROM comments c
             LEFT JOIN posts po ON c.target_type = 'POST' AND c.target_id = po.id
             LEFT JOIN notices nt ON c.target_type = 'NOTICE' AND c.target_id = nt.id
    WHERE c.user_id IS DISTINCT
FROM COALESCE (po.user_id, nt.user_id)
    ), daily AS (
-- 유저 x 날짜 단위로 합산한 뒤 일일 상한 500을 씌운다.
SELECT user_id, LEAST(SUM (exp), 500) AS exp
FROM activity
GROUP BY user_id, activity_date
    ),
    total AS (
-- 상한이 적용된 일별 exp를 유저 단위로 합산 = 최종 누적 exp
SELECT user_id, SUM (exp) AS exp
FROM daily
GROUP BY user_id
    )
INSERT
INTO user_levels (user_id, exp, exp_earned_today, exp_earned_date)
SELECT u.id,
       COALESCE(t.exp, 0),
       -- exp_earned_today / exp_earned_date는 반드시 0 / NULL로 둔다.
       --   백필분을 "오늘 획득량"에 넣어버리면 백필받은 유저가 배포 당일에는
       --   상한(500)에 이미 걸린 상태가 되어 활동해도 exp를 못 받는다.
       --   NULL이면 UserLevel.gainExp()의 날짜 비교에서 무조건 불일치 처리되어
       --   첫 활동 시 오늘치 카운터가 깨끗하게 초기화된다.
       0,
       NULL
FROM users u
         LEFT JOIN total t ON t.user_id = u.id ON CONFLICT (user_id) DO NOTHING;
