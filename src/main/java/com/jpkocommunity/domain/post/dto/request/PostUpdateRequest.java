package com.jpkocommunity.domain.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostUpdateRequest(

        @NotNull(message = "카테고리를 선택해주세요.")
        Long categoryId,

        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        @NotBlank(message = "내용을 입력해주세요.")
        @Size(max = 20000, message = "본문은 20,000자 이하여야 합니다.")
        String content
) {}
