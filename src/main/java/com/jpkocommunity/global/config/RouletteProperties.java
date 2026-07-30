package com.jpkocommunity.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 일일 포인트 룰렛 설정
 */
@ConfigurationProperties(prefix = "app.roulette")
public record RouletteProperties(
        long minAmount,
        long maxAmount
) {
}
