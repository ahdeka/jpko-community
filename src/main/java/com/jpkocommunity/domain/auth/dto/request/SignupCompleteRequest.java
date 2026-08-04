package com.jpkocommunity.domain.auth.dto.request;

import jakarta.validation.constraints.*;

public record SignupCompleteRequest(

        @NotBlank(message = "토큰이 필요합니다.")
        String token,

        @NotBlank(message = "아이디를 입력해주세요.")
        @Pattern(regexp = "^[a-z0-9_]{4,20}$", message = "아이디는 영문 소문자, 숫자, 언더바(4~20자)만 사용할 수 있습니다.")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, max = 50, message = "비밀번호는 8~50자 사이여야 합니다.")
        String password,

        @NotBlank(message = "비밀번호 확인을 입력해주세요.")
        String passwordConfirm,

        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.")
        String nickname,

        @AssertTrue(message = "이용약관에 동의해주세요.")
        @NotNull(message = "이용약관 동의 여부를 입력해주세요.")
        Boolean termsAgreed,

        @AssertTrue(message = "개인정보처리방침에 동의해주세요.")
        @NotNull(message = "개인정보처리방침 동의 여부를 입력해주세요.")
        Boolean privacyAgreed
) {
    public SignupCompleteRequest {
        loginId = loginId == null ? null : loginId.trim().toLowerCase();
        nickname = nickname == null ? null : nickname.trim();
    }
}
