package com.jpkocommunity.domain.user.dto.request;

import com.jpkocommunity.domain.user.entity.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserStatusRequest(
        @NotNull(message = "상태를 선택해주세요.")
        UserStatus status
) {}
