package com.jpkocommunity.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 기존 유저들 회원가입 마이그레이션 요청용
 * TODO 마이그레이션 끝나면 정리될 파일
 */
public record SignupMigrationRequest(

        @NotBlank(message = "아이디를 입력해주세요.")
        @Pattern(regexp = "^[a-z0-9_]{4,20}$", message = "아이디는 영문 소문자, 숫자, 언더바(4~20자)만 사용할 수 있습니다.")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, max = 50, message = "비밀번호는 8~50자 사이여야 합니다.")
        String password,

        @NotBlank(message = "비밀번호 확인을 입력해주세요.")
        String passwordConfirm
) {
    public SignupMigrationRequest {
        loginId = loginId == null ? null : loginId.trim().toLowerCase();
    }
}
