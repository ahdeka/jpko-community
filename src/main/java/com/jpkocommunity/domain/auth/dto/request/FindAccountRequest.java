package com.jpkocommunity.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record FindAccountRequest(

        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "보안 문자 인증이 필요합니다.")
        String turnstileToken
) {
    public FindAccountRequest {
        email = email == null ? null : email.trim().toLowerCase();
    }
}
