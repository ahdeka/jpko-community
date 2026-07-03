package com.jpkocommunity.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(

        @NotBlank(message = "토큰이 필요합니다.")
        String token,

        @NotBlank(message = "새 비밀번호를 입력해주세요.")
        @Size(min = 8, max = 50, message = "비밀번호는 8~50자 사이여야 합니다.")
        String newPassword,

        @NotBlank(message = "비밀번호 확인을 입력해주세요.")
        String newPasswordConfirm
) {}
