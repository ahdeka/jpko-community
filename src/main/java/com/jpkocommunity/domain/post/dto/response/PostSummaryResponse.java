package com.jpkocommunity.domain.post.dto.response;

import com.jpkocommunity.domain.post.entity.Post;
import com.jpkocommunity.domain.user.entity.UserRole;

import java.time.LocalDateTime;

public record PostSummaryResponse(
        Long id,
        String categoryName,
        String title,
        String author,
        Integer authorLevel,
        boolean adminAuthor,
        boolean rootAuthor,
        boolean anonymous,
        int viewCount,
        long commentCount,
        long likeCount,
        boolean hasImage,
        LocalDateTime createdAt
) {
    public static PostSummaryResponse from(Post post, long commentCount, long likeCount, boolean hasImage, Integer authorLevel) {
        return new PostSummaryResponse(
                post.getId(),
                post.getCategory().getName(),
                post.getTitle(),
                resolveAuthor(post),
                resolveAuthorLevel(post, authorLevel),
                resolveAdminAuthor(post),
                resolveRootAuthor(post),
                post.isAnonymous(),
                post.getViewCount(),
                commentCount,
                likeCount,
                hasImage,
                post.getCreatedAt()
        );
    }

    private static Integer resolveAuthorLevel(Post post, Integer level) {
        if (post.isAnonymous() || post.getUser().isDeleted()) return null;
        return level != null ? level : 0;
    }

    private static String resolveAuthor(Post post) {
        if (post.isAnonymous()) return "ㅇㅇ(" + post.getMaskedIp() + ")";
        return post.getUser().getDisplayNickname();
    }

    // 작성자가 ADMIN 인지 확인
    private static boolean resolveAdminAuthor(Post post) {
        return !post.isAnonymous() && post.getUser().getRole() == UserRole.ADMIN;
    }

    // 작성자가 ROOT 인지 확인
    private static boolean resolveRootAuthor(Post post) {
        return !post.isAnonymous() && post.getUser().getRole() == UserRole.ROOT;
    }
}
