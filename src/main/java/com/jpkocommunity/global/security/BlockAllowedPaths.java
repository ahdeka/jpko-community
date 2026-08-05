package com.jpkocommunity.global.security;

import java.util.Set;

// 임시 활동 차단 상태인 계정도 예외적으로 호출을 허용하는 경로
// (차단 사유 확인, 조기 해제, 관리자에게 이의제기)
public final class BlockAllowedPaths {

    private BlockAllowedPaths() {}

    public static final Set<String> PATHS = Set.of(
            "/api/shop/blocks/me",
            "/api/shop/blocks/me/unblock",
            "/api/shop/blocks/me/appeal",
            "/api/auth/logout"
    );
}
