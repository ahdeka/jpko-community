-- ============================================================
-- V43: 닉네임 대소문자 무시 기준으로 전환
--   사용자가 입력한 대소문자는 그대로 저장됨
--   기존 UNIQUE (nickname) 방식 Summer =/ summer
--   신규 UNIQUE (lower(nickname)) 방식 Summer = summer
--   본인이 수정하는건 OK (ex. Summer -> sUmmer)
-- ============================================================

-- 1) 혹시 모를 사전 점검
DO $$
DECLARE
conflicts text;
BEGIN
SELECT string_agg(grp, ' | ')
INTO conflicts
FROM (
         SELECT lower(nickname) || ' -> ' ||
                string_agg(nickname || '(id=' || id::text || ')', ', ' ORDER BY id) AS grp
         FROM users
         GROUP BY lower(nickname)
         HAVING count(*) > 1
     ) c;

IF conflicts IS NOT NULL THEN
        RAISE EXCEPTION 'V43 중단: 대소문자 무시 시 중복되는 닉네임이 남아 있습니다 -> %', conflicts;
END IF;
END $$;

-- 2) 신규 제약 생성
CREATE UNIQUE INDEX uk_users_nickname_ci ON users (lower(nickname));

-- 3) 기존 완전일치 제약 제거
ALTER TABLE users DROP CONSTRAINT uk_users_nickname;
