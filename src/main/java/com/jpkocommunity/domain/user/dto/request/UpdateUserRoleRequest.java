package com.jpkocommunity.domain.user.dto.request;

import com.jpkocommunity.domain.user.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull(message = "유저 권한을 선택해주세요.")
        UserRole role
) {
}
