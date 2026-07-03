package com.jpkocommunity.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "verification-token")
public record VerificationTokenProperties(
        long emailVerificationExpiration,
        long passwordResetExpiration
) {
}