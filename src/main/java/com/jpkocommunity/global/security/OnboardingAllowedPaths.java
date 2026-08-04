package com.jpkocommunity.global.security;

import java.util.Set;

/**
 * 온보딩 미완료 계정도 예외적으로 쓰기 요청이 허용되는 경로
 * TODO 마이그레이션 끝나면 정리될 파일
 */
public final class OnboardingAllowedPaths {

    public static final Set<String> PATHS = Set.of(
            "/api/auth/logout",
            "/api/auth/complete-onboarding",
            "/api/auth/email-verification/request",
            "/api/auth/email-verification/confirm",
            "/api/auth/email"
    );
}