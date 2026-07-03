package com.jpkocommunity.global.security;

import java.util.Set;

// SecurityConfig랑 Jwt필터에서 사용
public final class PublicAuthPaths {

    private PublicAuthPaths() {}

    // refresh/logout: 만료된 accessToken으로 요청이 오므로 필터에서 제외 필수
    // login/signup: 토큰 없어도 무해하지만 명시적으로 제외
    public static final Set<String> PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/signup",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/email-verification/confirm",
            "/api/auth/email-verification/resend",
            "/api/auth/password-reset/request",
            "/api/auth/password-reset/confirm"
    );
}