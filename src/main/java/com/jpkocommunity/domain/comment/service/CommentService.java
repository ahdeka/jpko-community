package com.jpkocommunity.domain.comment.service;

import com.jpkocommunity.domain.comment.dto.request.CommentCreateRequest;
import com.jpkocommunity.domain.comment.dto.request.CommentUpdateRequest;
import com.jpkocommunity.domain.comment.dto.response.CommentPageResponse;
import com.jpkocommunity.domain.comment.dto.response.CommentResponse;
import com.jpkocommunity.domain.comment.entity.Comment;
import com.jpkocommunity.domain.comment.entity.CommentTargetType;
import com.jpkocommunity.domain.comment.entity.Commentable;
import com.jpkocommunity.domain.comment.repository.CommentRepository;
import com.jpkocommunity.domain.image.service.ImageService;
import com.jpkocommunity.domain.notice.service.NoticeService;
import com.jpkocommunity.domain.notification.entity.NotificationType;
import com.jpkocommunity.domain.notification.event.NotificationEvent;
import com.jpkocommunity.domain.notification.repository.NotificationRepository;
import com.jpkocommunity.domain.point.entity.GrantType;
import com.jpkocommunity.domain.point.service.PointService;
import com.jpkocommunity.domain.post.service.PostService;
import com.jpkocommunity.domain.report.entity.ReportTargetType;
import com.jpkocommunity.domain.report.event.ContentDeletedEvent;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserRole;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.config.ThrottleLockClassifier;
import com.jpkocommunity.global.config.properties.PointProperties;
import com.jpkocommunity.global.config.properties.ThrottleProperties;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import com.jpkocommunity.global.security.auth.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostService postService;
    private final UserService userService;
    private final ImageService imageService;
    private final ApplicationEventPublisher eventPublisher;
    private final ThrottleProperties throttleProperties;
    private final PointProperties pointProperties;
    private final PointService pointService;
    private final NoticeService noticeService;
    private final NotificationRepository notificationRepository;

    public CommentPageResponse getComments(CommentTargetType type, Long targetId, Long currentUserId, Pageable pageable) {
        loadTarget(type, targetId);
        Page<CommentResponse> page = commentRepository.findTopLevelByTarget(type, targetId, pageable)
                .map(comment -> CommentResponse.from(comment, currentUserId));
        long total = commentRepository.countByTarget(type, targetId);
        return CommentPageResponse.of(page, total);
    }

    @Transactional
    public CommentResponse createComment(
            Long userId, CommentTargetType type, Long targetId,
            CommentCreateRequest request, String ipAddress
    ) {
        User user = userService.findById(userId);
        validateThrottle(user);

        Commentable target = loadTarget(type, targetId);
        Comment parent = resolveParent(request.parentId(), type, targetId);

        Comment comment = saveComment(type, targetId, user, parent, request.content(), request.imageUrl(), request.anonymous(), ipAddress);

        // 공지 댓글은 알림 대상에서 제외
        if (type == CommentTargetType.POST) {
            publishNotificationEvent(comment, target, parent, userId);
            grantCommentPointIfEligible(userId, target, comment, targetId);
        }

        return request.parentId() == null
                ? CommentResponse.from(comment, userId)
                : CommentResponse.fromReply(comment, userId);
    }

    private Commentable loadTarget(CommentTargetType type, Long targetId) {
        return switch (type) {
            case POST -> postService.findActivePostById(targetId);
            case NOTICE -> noticeService.findActiveNotice(targetId);
        };
    }

    @Transactional
    public CommentResponse updateComment(AuthUser authUser, Long commentId,
                                         CommentUpdateRequest request) {
        Comment comment = findActiveComment(commentId);

        if (!comment.getUser().getId().equals(authUser.userId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        comment.update(request.content());
        return CommentResponse.from(comment, authUser.userId());
    }

    @Transactional
    public void deleteComment(AuthUser authUser, Long commentId) {
        Comment comment = findActiveComment(commentId);
        validateAuthor(comment, authUser);

        Long commentAuthorId = comment.getUser().getId();
        CommentTargetType type = comment.getTargetType();
        Long targetId = comment.getTargetId();

        applyDelete(comment, commentId);

        eventPublisher.publishEvent(new ContentDeletedEvent(ReportTargetType.COMMENT, commentId));
        if (type == CommentTargetType.POST) {
            notifyIfRemovedByAdmin(authUser, commentAuthorId, targetId, commentId);
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
        boolean isPrivileged = authUser.role() == UserRole.ROOT || authUser.role() == UserRole.ADMIN;
        if (!isOwner && !isPrivileged) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private Comment saveComment(CommentTargetType type, Long targetId, User user, Comment parent, String content, String tempImageUrl, boolean anonymous, String ipAddress) {
        boolean hasImage = tempImageUrl != null && !tempImageUrl.isEmpty();
        boolean hasText = StringUtils.hasText(content);

        if (!hasText && !hasImage) throw new CustomException(ErrorCode.INVALID_INPUT);

        String imageUrl = hasImage
                ? imageService.moveTempMediaToComment(tempImageUrl)
                : null;

        return commentRepository.save(Comment.builder()
                .targetType(type)
                .targetId(targetId)
                .user(user)
                .parent(parent)
                .content(hasText ? content : "")
                .imageUrl(imageUrl)
                .anonymous(anonymous)
                .ipAddress(ipAddress)
                .build());
    }

    private Comment resolveParent(Long parentId, CommentTargetType type, Long targetId) {
        if (parentId == null) return null;

        Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        // 1. 삭제 여부 확인
        if (parent.isDeleted()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // 2. 같은 게시글에 속한 댓글인지 확인
        if (!parent.isSameTarget(type, targetId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // 3. 대댓글에 대댓글 방지
        if (parent.getParent() != null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        return parent;
    }

    // 댓글은 게시글 작성자에게 알림, 대댓글은 부모 댓글 작성자에게 알림
    private void publishNotificationEvent(Comment saved, Commentable target, Comment parent, Long commenterId) {
        Long receiverId = (parent == null) ? target.getAuthor().getId() : parent.getUser().getId();

        // 작성자와 수신자가 같으면 알림 발송하지 않음
        if (receiverId.equals(commenterId)) return;

        NotificationType type = (parent == null) ? NotificationType.COMMENT : NotificationType.REPLY;
        eventPublisher.publishEvent(new NotificationEvent(
                receiverId, commenterId, type, target.getId(), saved.getId(), saved.isAnonymous(), null
        ));
    }

    private void grantCommentPointIfEligible(Long userId, Commentable target, Comment comment, Long postId) {
        if (target.getAuthor().getId().equals(userId)) return;

        int point = pointProperties.getPoints().get("create-comment");
        pointService.grantPointByActivity(userId, point, "Comment: " + comment.getId(),
                postId, comment.getId(), GrantType.COMMENT);
    }

    // Throttling 검증
    private void validateThrottle(User user) {
        if (user.getRole() == UserRole.ADMIN) return;

        commentRepository.acquireUserLock(ThrottleLockClassifier.COMMENT, user.getId());

        commentRepository.findLatestCreatedAtByUserId(user.getId())
                .filter(last -> last.isAfter(LocalDateTime.now().minusSeconds(throttleProperties.commentIntervalSeconds())))
                .ifPresent(last -> {
                    throw new CustomException(ErrorCode.COMMENT_TOO_FREQUENT);
                });
    }

    // 대댓글이 살아 있으면 소프트 삭제, 없으면 부모까지 하드 삭제
    private void applyDelete(Comment comment, Long commentId) {
        boolean hasLiveReplies = comment.getReplies().stream().anyMatch(reply -> !reply.isDeleted());
        if (hasLiveReplies) {
            comment.delete();
            return;
        }

        Comment parent = comment.getParent();

        // 부모가 이미 삭제됐고 이 댓글 외 살아있는 대댓글이 없으면 부모 서브트리를 통째로 하드 삭제
        if (parent != null && parent.isDeleted() && !hasOtherLiveReplies(parent, commentId)) {
            hardDelete(parent);
        } else {
            hardDelete(comment);
        }
    }

    private boolean hasOtherLiveReplies(Comment parent, Long commentId) {
        return parent.getReplies().stream()
                .anyMatch(reply -> !reply.isDeleted() && !reply.getId().equals(commentId));
    }

    private void hardDelete(Comment comment) {
        List<Comment> replies = comment.getReplies();

        // 물리 삭제 전에 알림 삭제 (참조성 해결)
        List<Long> deletingIds = Stream.concat(
                        Stream.of(comment.getId()),
                        replies.stream().map(Comment::getId))
                .toList();
        notificationRepository.deleteByCommentIdIn(deletingIds);

        List<String> imageUrls = Stream.concat(Stream.of(comment), replies.stream())
                .map(Comment::getImageUrl)
                .filter(Objects::nonNull)
                .toList();
        imageService.deleteCommentImages(imageUrls);

        if (!replies.isEmpty()) {
            commentRepository.deleteAll(replies);
        }
        commentRepository.delete(comment);
    }

    private void notifyIfRemovedByAdmin(AuthUser authUser, Long authorId, Long postId, Long commentId) {
        boolean isPrivileged = authUser.role() == UserRole.ROOT || authUser.role() == UserRole.ADMIN;
        boolean isOwner = authorId.equals(authUser.userId());

        if (isPrivileged && !isOwner) {
            eventPublisher.publishEvent(new NotificationEvent(
                    authorId, authUser.userId(), NotificationType.CONTENT_REMOVED,
                    postId, commentId, false, null));
        }
    }
}
