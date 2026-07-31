package com.jpkocommunity.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Throttling 설정 (글 / 댓글)
 */
@ConfigurationProperties(prefix = "app.throttle")
public record ThrottleProperties(
        long postIntervalSeconds,
        long commentIntervalSeconds
) {
}