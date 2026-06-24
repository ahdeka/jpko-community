package com.jpkocommunity.domain.user.controller;

import com.jpkocommunity.domain.user.dto.request.UpdateNicknameRequest;
import com.jpkocommunity.domain.user.dto.request.UpdatePasswordRequest;
import com.jpkocommunity.domain.user.dto.response.MyCommentResponse;
import com.jpkocommunity.domain.user.dto.response.MyPostResponse;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me/posts")
    public ResponseEntity<ApiResponse<Page<MyPostResponse>>> getMyPosts(
            @AuthenticationPrincipal AuthUser authUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.getMyPosts(authUser.userId(), pageable)));
    }

    @GetMapping("/me/comments")
    public ResponseEntity<ApiResponse<Page<MyCommentResponse>>> getMyComments(
            @AuthenticationPrincipal AuthUser authUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.getMyComments(authUser.userId(), pageable)));
    }

    @PatchMapping("/me/nickname")
    public ResponseEntity<ApiResponse<Void>> updateNickname(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody UpdateNicknameRequest request
    ) {
        userService.updateNickname(authUser.userId(), request);
        return ResponseEntity.ok(ApiResponse.ok("닉네임이 변경되었습니다."));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody UpdatePasswordRequest request
    ) {
        userService.updatePassword(authUser.userId(), request);
        return ResponseEntity.ok(ApiResponse.ok("비밀번호가 변경되었습니다."));
    }

}
