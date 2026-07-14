package com.jpkocommunity.domain.post.dto.response;

import com.jpkocommunity.domain.post.entity.Post;
import com.jpkocommunity.domain.user.entity.UserRole;

import java.time.LocalDateTime;

public record PostSummaryResponse(
        Long id,
        String categoryName,
        String title,
        String author,
        boolean adminAuthor,
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
                resolveAdminAuthor(post),
                post.isAnonymous(),
                post.getViewCount(),
                commentCount,
                likeCount,
                hasImage,
                post.getCreatedAt()
        );
    }

    private static String resolveAuthor(Post post) {
        if (post.isAnonymous()) return "ㅇㅇ(" + post.getMaskedIp() + ")";
        return post.getUser().getDisplayNickname();
    }

    // 작성자가 운영진(ADMIN)인지 — 닉네임 옆 "운영진" 뱃지 표시에만 쓴다.
    // 익명 글이면 false: 운영진이 익명으로 써도 신원이 드러나면 안 되기 때문.
    private static boolean resolveAdminAuthor(Post post) {
        return !post.isAnonymous() && post.getUser().getRole() == UserRole.ADMIN;
    }
}
