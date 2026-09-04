package com.jpkocommunity.global.config.properties;

import com.jpkocommunity.domain.level.entity.ExpAction;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 레벨 관련 프로퍼티
 */
@ConfigurationProperties(prefix = "app.exp")
public record LevelProperties(
        long dailyLimit,
        long createPost,
        long createComment
) {
    public LevelProperties {
        if (dailyLimit < 0) {
            throw new IllegalArgumentException("app.exp.daily-limit는 음수일 수 없습니다: " + dailyLimit);
        }
        if (createPost < 0 || createComment < 0) {
            throw new IllegalArgumentException("app.exp의 활동별 획득량은 음수일 수 없습니다.");
        }
    }

    public long expOf(ExpAction action) {
        return switch (action) {
            case CREATE_POST -> createPost;
            case CREATE_COMMENT -> createComment;
        };
    }
}
