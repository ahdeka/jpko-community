package com.jpkocommunity.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpiration,
        long refreshTokenExpiration
) {

    // 쿠키 Max-Age는 초 단위라 ms로 변환
    public int accessTokenMaxAgeSeconds() {
        return (int) (accessTokenExpiration / 1000);
    }

    public int refreshTokenMaxAgeSeconds() {
        return (int) (refreshTokenExpiration / 1000);
    }
}