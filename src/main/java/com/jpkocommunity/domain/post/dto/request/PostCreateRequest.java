package com.jpkocommunity.domain.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostCreateRequest(

        @NotNull(message = "카테고리를 선택해주세요.")
        Long categoryId,

        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        @NotBlank(message = "내용을 입력해주세요.")
        String content,

        // 익명 여부 - 기본값 false (닉네임 표시)
        boolean anonymous,

        // 태그는 선택사항 - null이면 빈 리스트로 처리
        List<String> tags
) {}