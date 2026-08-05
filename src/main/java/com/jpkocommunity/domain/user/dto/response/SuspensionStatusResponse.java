package com.jpkocommunity.domain.user.dto.response;

import java.time.LocalDateTime;

// 본인의 정지(SUSPENDED) 현황 (GET /api/users/me/suspension). suspended=false면 나머지는 무시된다.
public record SuspensionStatusResponse(boolean suspended, LocalDateTime suspendedUntil, Long unlockPrice) {

    public static SuspensionStatusResponse notSuspended() {
        return new SuspensionStatusResponse(false, null, null);
    }

    public static SuspensionStatusResponse suspended(LocalDateTime suspendedUntil, Long unlockPrice) {
        return new SuspensionStatusResponse(true, suspendedUntil, unlockPrice);
    }
}
