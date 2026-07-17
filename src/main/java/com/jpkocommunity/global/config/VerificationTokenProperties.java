package com.jpkocommunity.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 검증 토큰 만료 시간 설정
 */
@ConfigurationProperties(prefix = "verification-token")
public record VerificationTokenProperties(
        long emailVerificationExpiration,
        long passwordResetExpiration
) {
}