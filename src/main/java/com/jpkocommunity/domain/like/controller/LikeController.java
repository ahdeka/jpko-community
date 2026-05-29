package com.jpkocommunity.domain.like.controller;

import com.jpkocommunity.domain.like.dto.request.LikeRequest;
import com.jpkocommunity.domain.like.dto.response.LikeResponse;
import com.jpkocommunity.domain.like.service.LikeService;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping
    public ResponseEntity<ApiResponse<LikeResponse>> toggle(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long postId,
            @Valid @RequestBody LikeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                likeService.toggle(authUser.userId(), postId, request)
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<LikeResponse>> getLikeStatus(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long postId
    ) {
        // 비로그인이면 authUser가 null → myType은 null로 응답
        Long userId = authUser != null ? authUser.userId() : null;
        return ResponseEntity.ok(ApiResponse.ok(
                likeService.getLikeStatus(postId, userId)
        ));
    }
}