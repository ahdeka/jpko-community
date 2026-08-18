package com.jpkocommunity.domain.comment.dto.response;

import org.springframework.data.domain.Page;

public record CommentPageResponse(
        Page<CommentResponse> comments,
        long totalCommentCount            // 대댓글 포함, 헤더 표시용
) {
    public static CommentPageResponse of(Page<CommentResponse> page, long total) {
        return new CommentPageResponse(page, total);
    }
}