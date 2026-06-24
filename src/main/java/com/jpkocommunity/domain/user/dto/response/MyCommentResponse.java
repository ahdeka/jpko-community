package com.jpkocommunity.domain.user.dto.response;

import com.jpkocommunity.domain.comment.entity.Comment;

import java.time.LocalDateTime;

public record MyCommentResponse(
        Long id,
        Long postId,          // 클릭 시 해당 게시글로 이동하기 위해
        String postTitle,     // 어느 글에 단 댓글인지 표시
        String content,
        LocalDateTime createdAt
) {
    public static MyCommentResponse from(Comment comment) {
        return new MyCommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getPost().getTitle(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}