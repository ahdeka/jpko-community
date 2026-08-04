package com.jpkocommunity.domain.user.dto.response;

import java.time.LocalDateTime;

// SUSPENDED_ACCOUNT 에러 응답의 부가 데이터. suspendedUntil이 null이면 무기한 정지(ROOT만 가능).
public record SuspendedAccountResponse(LocalDateTime suspendedUntil) {}
