package com.jpkocommunity.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        // TODO 마이그레이션이 완료되면 메시지 수정 필요
        @NotBlank(message = "아이디 또는 이메일을 입력해주세요.")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password,

        @NotBlank(message = "보안 문자 인증이 필요합니다.")
        String turnstileToken
) {
    public LoginRequest {
        loginId = loginId == null ? null : loginId.trim().toLowerCase();
    }
}
