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
        boolean isOwner,
        int viewCount,
        long likeCount,
        long dislikeCount,
        LocalDateTime createdAt
) {
    // viewCount는 엔티티(post.getViewCount())가 아니라 호출부에서 넘겨받는다.
    // 조회수 증가를 벌크 UPDATE로 처리하면 로드된 엔티티에는 +1이 반영되지 않으므로,
    // 표시용 조회수를 명시적으로 전달받아야 정확한 값이 응답된다.
    public static PostDetailResponse from(
            Post post, int viewCount, long likeCount, long dislikeCount, Long currentUserId) {
        return new PostDetailResponse(
                post.getId(),
                post.getCategory().getName(),
                post.getTitle(),
                post.getContent(),
                resolveAuthor(post),
                post.isAnonymous(),
                isOwner(post, currentUserId),
                viewCount,
                likeCount,
                dislikeCount,
                post.getCreatedAt()
        );
    }

    private static String resolveAuthor(Post post) {
        if (post.isAnonymous()) return "ㅇㅇ(" + post.getMaskedIp() + ")";
        return post.getUser().getNickname();
    }

    // 익명 글이라도 실제 작성자 본인이면 true
    private static boolean isOwner(Post post, Long currentUserId) {
        return currentUserId != null && post.getUser().getId().equals(currentUserId);
    }
}
