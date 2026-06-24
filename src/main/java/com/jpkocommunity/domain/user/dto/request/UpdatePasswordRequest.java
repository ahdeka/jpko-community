package com.jpkocommunity.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePasswordRequest(
        @NotBlank(message = "현재 비밀번호를 입력해주세요.")
        String currentPassword,

        @NotBlank(message = "새 비밀번호를 입력해주세요.")
        @Size(min = 8, max = 20, message = "비밀번호는 8~20자 사이여야 합니다.")
        String newPassword,

        @NotBlank(message = "새 비밀번호 확인을 입력해주세요.")
        String newPasswordConfirm
) {}
