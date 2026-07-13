package com.jpkocommunity.domain.user.dto.request;

import com.jpkocommunity.domain.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull(message = "상태를 선택해주세요.")
        UserStatus status
) {}
