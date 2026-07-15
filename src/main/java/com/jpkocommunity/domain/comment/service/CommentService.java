package com.jpkocommunity.domain.comment.service;

import com.jpkocommunity.domain.comment.dto.request.CommentCreateRequest;
import com.jpkocommunity.domain.comment.dto.request.CommentUpdateRequest;
import com.jpkocommunity.domain.comment.dto.response.CommentResponse;
import com.jpkocommunity.domain.comment.entity.Comment;
import com.jpkocommunity.domain.comment.repository.CommentRepository;
import com.jpkocommunity.domain.notification.entity.NotificationType;
import com.jpkocommunity.domain.notification.event.NotificationEvent;
import com.jpkocommunity.domain.post.entity.Post;
import com.jpkocommunity.domain.post.service.PostService;
import com.jpkocommunity.domain.report.entity.ReportTargetType;
import com.jpkocommunity.domain.report.event.ContentDeletedEvent;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserRole;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import com.jpkocommunity.global.security.auth.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostService postService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    public List<CommentResponse> getComments(Long postId, Long currentUserId) {
        postService.findActivePostById(postId);
        return commentRepository.findTopLevelWithRepliesByPostId(postId)
                .stream()
                .map(comment -> CommentResponse.from(comment, currentUserId))
                .toList();
    }

    @Transactional
    public CommentResponse createComment(Long userId, Long postId,
                                         CommentCreateRequest request, String ipAddress) {
        User user = userService.findById(userId);
        Post post = postService.findActivePostById(postId);

        Comment parent = resolveParent(request.parentId(), postId);

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .parent(parent)
                .content(request.content())
                .anonymous(request.anonymous())
                .ipAddress(ipAddress)
                .build();

        Comment saved = commentRepository.save(comment);
        publishNotificationEvent(saved, post, parent, userId);

        return request.parentId() == null
                ? CommentResponse.from(saved, userId)
                : CommentResponse.fromReply(saved, userId);
    }

    @Transactional
    public CommentResponse updateComment(Long userId, Long commentId,
                                         CommentUpdateRequest request) {
        Comment comment = findActiveComment(commentId);

        comment.update(request.content());
        return CommentResponse.from(comment, userId);
    }

    @Transactional
    public void deleteComment(AuthUser authUser, Long commentId) {
        Comment comment = findActiveComment(commentId);
        validateAuthor(comment, authUser);

        boolean isAdmin = authUser.role() == UserRole.ADMIN;
        boolean isOwner = comment.getUser().getId().equals(authUser.userId());

        comment.delete();

        eventPublisher.publishEvent(new ContentDeletedEvent(ReportTargetType.COMMENT, commentId));

        if (isAdmin && !isOwner) {
            eventPublisher.publishEvent(new NotificationEvent(
                    comment.getUser().getId(), authUser.userId(), NotificationType.CONTENT_REMOVED,
                    comment.getPost().getId(), commentId, false
            ));
        }
    }

    public Optional<Comment> findByIdOptional(Long commentId) {
        return commentRepository.findById(commentId);
    }

    // 신고 미리보기용 - id 목록으로 작성자까지 한 번에 조회
    public List<Comment> findAllWithUserByIdIn(List<Long> ids) {
        return commentRepository.findAllWithUserByIdIn(ids);
    }

    public Comment findActiveComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        if (comment.isDeleted()) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }
        return comment;
    }

    // ========== private 메서드 ==========

    private void validateAuthor(Comment comment, AuthUser authUser) {
        boolean isOwner = comment.getUser().getId().equals(authUser.userId());
        boolean isAdmin = authUser.role() == UserRole.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private Comment resolveParent(Long parentId, Long postId) {
        if (parentId == null) return null;

        Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        // 1. 삭제 여부 확인
        if (parent.isDeleted()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // 2. 같은 게시글에 속한 댓글인지 확인
        if (!parent.getPost().getId().equals(postId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // 3. 대댓글에 대댓글 방지
        if (parent.getParent() != null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        return parent;
    }

    // 댓글은 게시글 작성자에게 알림, 대댓글은 부모 댓글 작성자에게 알림
    private void publishNotificationEvent(Comment saved, Post post, Comment parent, Long commenterId) {
        Long receiverId = (parent == null) ? post.getUser().getId() : parent.getUser().getId();

        // 작성자와 수신자가 같으면 알림 발송하지 않음
        if (receiverId.equals(commenterId)) {
            return;
        }

        NotificationType type = (parent == null) ? NotificationType.COMMENT : NotificationType.REPLY;
        eventPublisher.publishEvent(new NotificationEvent(
                receiverId, commenterId, type, post.getId(), saved.getId(), saved.isAnonymous()
        ));
    }

}
