package com.jpkocommunity.domain.user.dto.response;

import com.jpkocommunity.domain.post.entity.Post;

import java.time.LocalDateTime;

public record UserPostResponse(
        Long id,
        String categoryName,
        String title,
        int viewCount,
        long commentCount,
        long likeCount,
        LocalDateTime createdAt
) {
    public static UserPostResponse from(Post post, long commentCount, long likeCount) {
        return new UserPostResponse(
                post.getId(),
                post.getCategory().getName(),
                post.getTitle(),
                post.getViewCount(),
                commentCount,
                likeCount,
                post.getCreatedAt()
        );
    }
}