package com.jpkocommunity.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cloudflare Turnstile 검증 설정
 */
@ConfigurationProperties(prefix = "turnstile")
public record TurnstileProperties(
        boolean enabled,
        String secretKey,
        String verifyUrl
) {
}