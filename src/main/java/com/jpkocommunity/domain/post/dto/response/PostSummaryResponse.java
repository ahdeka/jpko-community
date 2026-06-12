package com.jpkocommunity.domain.post.dto.response;

import com.jpkocommunity.domain.post.entity.Post;

import java.time.LocalDateTime;

public record PostSummaryResponse(
        Long id,
        String categoryName,
        String title,
        String author,
        boolean anonymous,
        int viewCount,
        long commentCount,
        long likeCount,
        boolean hasImage,
        LocalDateTime createdAt
) {
    public static PostSummaryResponse from(Post post, long commentCount, long likeCount, boolean hasImage) {
        return new PostSummaryResponse(
                post.getId(),
                post.getCategory().getName(),
                post.getTitle(),
                resolveAuthor(post),
                post.isAnonymous(),
                post.getViewCount(),
                commentCount,
                likeCount,
                hasImage,
                post.getCreatedAt()
        );
    }

    private static String resolveAuthor(Post post) {
        if (post.isAnonymous()) {
            return "ㅇㅇ(" + post.getMaskedIp() + ")";
        }
        return post.getUser().getNickname();
    }
}
