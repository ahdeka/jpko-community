package com.jpkocommunity.domain.user.dto.response;

import com.jpkocommunity.domain.post.entity.Post;

import java.time.LocalDateTime;

public record MyPostResponse(
        Long id,
        String categoryName,
        String title,
        int viewCount,
        LocalDateTime createdAt
) {
    public static MyPostResponse from(Post post) {
        return new MyPostResponse(
                post.getId(),
                post.getCategory().getName(),
                post.getTitle(),
                post.getViewCount(),
                post.getCreatedAt()
        );
    }
}