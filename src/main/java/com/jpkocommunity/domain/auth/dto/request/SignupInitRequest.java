package com.jpkocommunity.domain.auth.dto.request;

import jakarta.validation.constraints.*;

public record SignupInitRequest(

        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "보안 문자 인증이 필요합니다.")
        String turnstileToken
) {
    public SignupInitRequest {
        email = email == null ? null : email.trim().toLowerCase();
    }
}
