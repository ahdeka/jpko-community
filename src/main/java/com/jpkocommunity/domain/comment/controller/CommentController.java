package com.jpkocommunity.domain.comment.controller;

import com.jpkocommunity.domain.comment.dto.request.CommentCreateRequest;
import com.jpkocommunity.domain.comment.dto.request.CommentUpdateRequest;
import com.jpkocommunity.domain.comment.dto.response.CommentResponse;
import com.jpkocommunity.domain.comment.service.CommentService;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/api/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable Long postId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(commentService.getComments(postId)));
    }

    @PostMapping("/api/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request,
            HttpServletRequest servletRequest
    ) {
        String ipAddress = getClientIp(servletRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("댓글이 등록되었습니다.",
                        commentService.createComment(authUser.userId(), postId, request, ipAddress)));
    }

    @PutMapping("/api/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                commentService.updateComment(authUser.userId(), commentId, request)));
    }

    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(authUser.userId(), commentId);
        return ResponseEntity.ok(ApiResponse.ok("댓글이 삭제되었습니다."));
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}