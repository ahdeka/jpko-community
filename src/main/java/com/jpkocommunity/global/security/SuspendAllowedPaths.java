package com.jpkocommunity.global.security;

import java.util.Set;

// 관리자에 의해 정지(SUSPENDED)된 계정도 예외적으로 호출을 허용하는 경로.
// 정지 사유·기한을 확인하고 로그아웃하는 것까지만 열어둔다 — 해제는 ADMIN/ROOT만 할 수 있다.
public final class SuspendAllowedPaths {

    private SuspendAllowedPaths() {}

    public static final Set<String> PATHS = Set.of(
            "/api/users/me/suspension",
            "/api/auth/logout"
    );
}
