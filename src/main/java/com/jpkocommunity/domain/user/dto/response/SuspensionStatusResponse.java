package com.jpkocommunity.domain.user.dto.response;

import java.time.LocalDateTime;

// 본인의 정지(SUSPENDED) 현황 (GET /api/users/me/suspension). suspended=false면 나머지는 무시된다.
// suspendedUntil이 null이면 ROOT가 건 무기한 정지다.
public record SuspensionStatusResponse(boolean suspended, LocalDateTime suspendedUntil) {

    public static SuspensionStatusResponse notSuspended() {
        return new SuspensionStatusResponse(false, null);
    }

    public static SuspensionStatusResponse suspended(LocalDateTime suspendedUntil) {
        return new SuspensionStatusResponse(true, suspendedUntil);
    }
}
