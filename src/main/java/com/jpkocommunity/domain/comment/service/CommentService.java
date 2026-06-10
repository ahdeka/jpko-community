package com.jpkocommunity.domain.comment.service;

import com.jpkocommunity.domain.comment.dto.request.CommentCreateRequest;
import com.jpkocommunity.domain.comment.dto.request.CommentUpdateRequest;
import com.jpkocommunity.domain.comment.dto.response.CommentResponse;
import com.jpkocommunity.domain.comment.entity.Comment;
import com.jpkocommunity.domain.comment.repository.CommentRepository;
import com.jpkocommunity.domain.post.entity.Post;
import com.jpkocommunity.domain.post.service.PostService;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostService postService;
    private final UserService userService;

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

        return request.parentId() == null
                ? CommentResponse.from(commentRepository.save(comment), userId)
                : CommentResponse.fromReply(commentRepository.save(comment), userId);
    }

    @Transactional
    public CommentResponse updateComment(Long userId, Long commentId,
                                         CommentUpdateRequest request) {
        Comment comment = findActiveComment(commentId);

        comment.update(request.content());
        return CommentResponse.from(comment, userId);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = findActiveComment(commentId);
        validateAuthor(comment, userId);
        comment.delete();
    }

    // ========== private 메서드 ==========

    private Comment findActiveComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        if (comment.isDeleted()) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }
        return comment;
    }

    private void validateAuthor(Comment comment, Long userId) {
        if (!comment.getUser().getId().equals(userId)) {
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
}
