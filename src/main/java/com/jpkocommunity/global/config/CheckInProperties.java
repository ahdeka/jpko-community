package com.jpkocommunity.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 매일 로그인 출첵 설정. minAmount는 음수를 허용한다(마이너스 결과가 나올 수 있음).
 */
@ConfigurationProperties(prefix = "app.checkin")
public record CheckInProperties(
        long minAmount,
        long maxAmount
) {
    public CheckInProperties {
        if (minAmount > maxAmount) {
            throw new IllegalArgumentException(
                    "app.checkin.min-amount(%d)는 max-amount(%d)보다 클 수 없습니다".formatted(minAmount, maxAmount));
        }
    }
}
