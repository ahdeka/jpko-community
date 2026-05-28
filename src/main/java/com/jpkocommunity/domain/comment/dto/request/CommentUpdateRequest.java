package com.jpkocommunity.domain.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentUpdateRequest(

        @NotBlank(message = "내용을 입력해주세요.")
        @Size(max = 500, message = "댓글은 500자 이하여야 합니다.")
        String content
) {}