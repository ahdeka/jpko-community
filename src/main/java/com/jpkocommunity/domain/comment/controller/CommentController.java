package com.jpkocommunity.domain.comment.controller;

import com.jpkocommunity.domain.comment.dto.request.CommentCreateRequest;
import com.jpkocommunity.domain.comment.dto.response.CommentPageResponse;
import com.jpkocommunity.domain.comment.dto.response.CommentResponse;
import com.jpkocommunity.domain.comment.entity.CommentTargetType;
import com.jpkocommunity.domain.comment.service.CommentService;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import com.jpkocommunity.global.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final IpUtils ipUtils;

    @GetMapping("/api/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentPageResponse>> getPostComments(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthUser authUser,
            @PageableDefault(size = 20)Pageable pageable
            ) {
        Long currentUserId = authUser != null ? authUser.userId() : null;
        return ResponseEntity.ok(ApiResponse.ok(commentService.getComments(CommentTargetType.POST, postId, currentUserId, pageable)));
    }

    @GetMapping("/api/notices/{noticeId}/comments")
    public ResponseEntity<ApiResponse<CommentPageResponse>> getNoticeComments(
            @PathVariable Long noticeId,
            @AuthenticationPrincipal AuthUser authUser,
            @PageableDefault(size = 20)Pageable pageable
    ) {
        Long currentUserId = authUser != null ? authUser.userId() : null;
        return ResponseEntity.ok(ApiResponse.ok(commentService.getComments(CommentTargetType.NOTICE, noticeId, currentUserId, pageable)));
    }

    @PostMapping("/api/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createOnPost(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request,
            HttpServletRequest servletRequest
    ) {
        String ipAddress = ipUtils.getClientIp(servletRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("댓글이 등록되었습니다.",
                        commentService.createComment(authUser.userId(), CommentTargetType.POST, postId, request, ipAddress)));
    }

    @PostMapping("/api/notices/{noticeId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createOnNotice(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long noticeId,
            @Valid @RequestBody CommentCreateRequest request,
            HttpServletRequest servletRequest
    ) {
        String ipAddress = ipUtils.getClientIp(servletRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("댓글이 등록되었습니다.",
                        commentService.createComment(authUser.userId(), CommentTargetType.NOTICE, noticeId, request, ipAddress)));
    }

    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(authUser, commentId);
        return ResponseEntity.ok(ApiResponse.ok("댓글이 삭제되었습니다."));
    }

}