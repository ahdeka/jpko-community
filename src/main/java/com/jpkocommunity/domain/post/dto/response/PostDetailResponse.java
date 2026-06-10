package com.jpkocommunity.domain.post.dto.response;

import com.jpkocommunity.domain.post.entity.Post;

import java.time.LocalDateTime;

public record PostDetailResponse(
        Long id,
        String categoryName,
        String title,
        String content,
        String author,
        boolean anonymous,
        int viewCount,
        long likeCount,
        long dislikeCount,
        LocalDateTime createdAt
) {
    public static PostDetailResponse from(Post post, long likeCount, long dislikeCount) {
        return new PostDetailResponse(
                post.getId(),
                post.getCategory().getName(),
                post.getTitle(),
                post.getContent(),
                resolveAuthor(post),
                post.isAnonymous(),
                post.getViewCount(),
                likeCount,
                dislikeCount,
                post.getCreatedAt()
        );
    }

    private static String resolveAuthor(Post post) {
        if (post.isAnonymous()) return "ㅇㅇ(" + post.getMaskedIp() + ")";
        return post.getUser().getNickname();
    }
}
