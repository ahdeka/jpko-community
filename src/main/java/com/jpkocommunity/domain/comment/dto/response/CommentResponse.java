package com.jpkocommunity.domain.comment.dto.response;

import com.jpkocommunity.domain.comment.entity.Comment;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        String author,
        boolean anonymous,
        boolean isOwner,
        String content,
        boolean deleted,
        LocalDateTime createdAt,
        List<CommentResponse> replies  // 대댓글은 항상 빈 리스트
) {
    // 최상위 댓글 (replies 포함)
    public static CommentResponse from(Comment comment, Long currentUserId) {
        return new CommentResponse(
                comment.getId(),
                resolveAuthor(comment),
                comment.isAnonymous(),
                isOwner(comment, currentUserId),
                resolveContent(comment),
                comment.isDeleted(),
                comment.getCreatedAt(),
                comment.getReplies().stream()
                        .map(reply -> CommentResponse.fromReply(reply, currentUserId))
                        .toList()
        );
    }

    // 대댓글 (replies 없음 - 1단계만 지원)
    public static CommentResponse fromReply(Comment comment, Long currentUserId) {
        return new CommentResponse(
                comment.getId(),
                resolveAuthor(comment),
                comment.isAnonymous(),
                isOwner(comment, currentUserId),
                resolveContent(comment),
                comment.isDeleted(),
                comment.getCreatedAt(),
                List.of()
        );
    }

    private static String resolveContent(Comment comment) {
        return comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent();
    }

    private static String resolveAuthor(Comment comment) {
        // 삭제된 댓글은 작성자 노출 안 함
        if (comment.isDeleted()) return "(삭제됨)";
        if (comment.isAnonymous()) return "ㅇㅇ(" + comment.getMaskedIp() + ")";
        return comment.getUser().getDisplayNickname();
    }

    // 익명 댓글이라도 실제 작성자 본인이면 true
    private static boolean isOwner(Comment comment, Long currentUserId) {
        return currentUserId != null && comment.getUser().getId().equals(currentUserId);
    }
}