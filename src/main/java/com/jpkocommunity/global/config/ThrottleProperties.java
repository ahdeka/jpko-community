package com.jpkocommunity.global.config;

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