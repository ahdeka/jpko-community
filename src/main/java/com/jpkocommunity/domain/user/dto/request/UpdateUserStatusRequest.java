package com.jpkocommunity.domain.user.dto.request;

import com.jpkocommunity.domain.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record UpdateUserStatusRequest(
        @NotNull(message = "상태를 선택해주세요.")
        UserStatus status,
        // ADMIN이 정지할 때는 필수(최대 7일). ROOT는 선택(NULL이면 무기한 정지)
        LocalDateTime suspendedUntil
) {}
