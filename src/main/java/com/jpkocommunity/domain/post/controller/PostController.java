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
import com.jpkocommunity.global.util.CookieUtils;
import com.jpkocommunity.global.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    // 조회수 중복 방지 쿠키 유효시간
    private static final int VIEW_COOKIE_MAX_AGE = 60 * 60 * 24;

    private final PostService postService;
    private final CookieUtils cookieUtils;
    private final IpUtils ipUtils;

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
            @AuthenticationPrincipal AuthUser authUser,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        Long currentUserId = authUser != null ? authUser.userId() : null;

        String cookieName = "viewed_" + postId;
        boolean alreadyViewed = cookieUtils.exists(servletRequest, cookieName);

        PostDetailResponse response = postService.getPost(postId, currentUserId, !alreadyViewed);

        if (!alreadyViewed) {
            cookieUtils.add(servletResponse, cookieName, "1", VIEW_COOKIE_MAX_AGE);
        }

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PostCreateRequest request,
            HttpServletRequest servletRequest  // IP 추출용
    ) {
        String ipAddress = ipUtils.getClientIp(servletRequest);
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

}
