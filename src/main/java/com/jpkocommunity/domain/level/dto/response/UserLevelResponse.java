package com.jpkocommunity.domain.level.dto.response;

import com.jpkocommunity.domain.level.service.LevelPolicy;

public record UserLevelResponse(
        int level,
        long totalExp,
        long expIntoLevel,  // 현재 레벨 구간에서 모은 exp
        long expPerLevel  // 진행률 분모
) {
    public static UserLevelResponse of(long exp) {
        return new UserLevelResponse(
                LevelPolicy.levelOfExp(exp),
                exp,
                LevelPolicy.expIntoLevel(exp),
                LevelPolicy.EXP_PER_LEVEL
        );
    }
}
