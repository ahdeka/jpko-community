package com.jpkocommunity.domain.auth.dto.request;

import jakarta.validation.constraints.*;

public record EmailChangeRequest(

        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email

        // TODO 이메일 변경 기능 확장 시, 비밀번호 재확인용 필드 필요
) {
    public EmailChangeRequest {
        email = email == null ? null : email.trim().toLowerCase();
    }
}
