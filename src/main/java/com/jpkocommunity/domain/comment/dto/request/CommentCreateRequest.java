package com.jpkocommunity.domain.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(

        @NotBlank(message = "내용을 입력해주세요.")
        @Size(max = 500, message = "댓글은 500자 이하여야 합니다.")
        String content,

        boolean anonymous,

        // null이면 일반 댓글, 값 있으면 대댓글
        Long parentId
) {}