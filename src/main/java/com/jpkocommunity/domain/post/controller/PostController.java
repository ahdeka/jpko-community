package com.jpkocommunity.domain.post.controller;

import com.jpkocommunity.domain.post.dto.request.PostCreateRequest;
import com.jpkocommunity.domain.post.dto.request.PostUpdateRequest;
import com.jpkocommunity.domain.post.dto.request.SearchType;
import com.jpkocommunity.domain.post.dto.response.PostDetailResponse;
import com.jpkocommunity.domain.post.dto.response.PostListResponse;
import com.jpkocommunity.domain.post.dto.response.PostResponse;
import com.jpkocommunity.domain.post.dto.response.PostSummaryResponse;
import com.jpkocommunity.domain.post.service.PostService;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<ApiResponse<PostListResponse>> getAllPosts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(postService.getAllPosts(pageable)));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<PostListResponse>> getPostsByCategory(
            @PathVariable Long categoryId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(postService.getPostsByCategory(categoryId, pageable)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PostListResponse>> searchPosts(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "TITLE_CONTENT") SearchType type,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(postService.searchPosts(keyword, type, categoryId, pageable)));
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<PostSummaryResponse>>> getPopularPosts(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "6") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(postService.getPopularPosts(days, limit)));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        Long currentUserId = authUser != null ? authUser.userId() : null;
        return ResponseEntity.ok(ApiResponse.ok(postService.getPost(postId, currentUserId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PostCreateRequest request,
            HttpServletRequest servletRequest  // IP 추출용
    ) {
        String ipAddress = getClientIp(servletRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        "게시글이 생성되었습니다.",
                        postService.createPost(authUser.userId(), request, ipAddress)
                ));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                postService.updatePost(authUser, postId, request)));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long postId
    ) {
        postService.deletePost(authUser, postId);
        return ResponseEntity.ok(ApiResponse.ok("게시글이 삭제되었습니다."));
    }

    // AuthController와 동일한 IP 추출 로직
    // TODO: 나중에 공통 유틸 클래스로 분리 가능
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
