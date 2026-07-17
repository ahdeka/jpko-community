package com.jpkocommunity.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 관련 설정
 *  secret: JWT 서명에 사용할 비밀 키
 *  accessTokenExpiration: 액세스 토큰 만료 시간 (ms)
 *  refreshTokenExpiration: 리프레시 토큰 만료 시간 (ms)
 */
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