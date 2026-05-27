package com.jpkocommunity.domain.post.dto.response;

import com.jpkocommunity.domain.post.entity.Post;
import com.jpkocommunity.domain.tag.dto.response.TagResponse;

import java.time.LocalDateTime;
import java.util.List;

public record PostSummaryResponse(
        Long id,
        String categoryName,
        String title,
        String author,
        boolean anonymous,
        int viewCount,
        List<TagResponse> tags,
        LocalDateTime createdAt
) {
    public static PostSummaryResponse from(Post post) {
        return new PostSummaryResponse(
                post.getId(),
                post.getCategory().getName(),
                post.getTitle(),
                resolveAuthor(post),
                post.isAnonymous(),
                post.getViewCount(),
                post.getPostTags().stream()
                        .map(pt -> TagResponse.from(pt.getTag()))
                        .toList(),
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