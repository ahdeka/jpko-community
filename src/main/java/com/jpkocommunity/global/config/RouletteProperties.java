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
    public RouletteProperties {
        if (minAmount < 0) {
            throw new IllegalArgumentException("app.roulette.min-amount는 0 이상이어야 합니다: " + minAmount);
        }
        if (minAmount > maxAmount) {
            throw new IllegalArgumentException(
                    "app.roulette.min-amount(%d)는 max-amount(%d)보다 클 수 없습니다".formatted(minAmount, maxAmount));
        }
    }
}
