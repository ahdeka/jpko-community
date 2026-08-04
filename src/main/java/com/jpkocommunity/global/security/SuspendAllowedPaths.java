package com.jpkocommunity.global.security;

import java.util.Set;

// 관리자에 의해 정지(SUSPENDED)된 계정도 예외적으로 호출을 허용하는 경로
// (정지 현황 확인, 포인트로 즉시 해제). BlockAllowedPaths(임시 활동 차단)와 같은 패턴이지만
// 대상 상태가 다르므로 별도로 둔다 — 정지와 차단은 서로 완전히 다른 메커니즘이다.
public final class SuspendAllowedPaths {

    private SuspendAllowedPaths() {}

    public static final Set<String> PATHS = Set.of(
            "/api/users/me/suspension",
            "/api/users/me/suspension/unlock",
            "/api/auth/logout"
    );
}
