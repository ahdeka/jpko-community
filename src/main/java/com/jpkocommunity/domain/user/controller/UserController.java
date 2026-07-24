package com.jpkocommunity.domain.user.controller;

import com.jpkocommunity.domain.report.dto.response.MyReportResponse;
import com.jpkocommunity.domain.report.service.ReportService;
import com.jpkocommunity.domain.user.dto.request.UpdateBioRequest;
import com.jpkocommunity.domain.user.dto.request.UpdateNicknameRequest;
import com.jpkocommunity.domain.user.dto.request.UpdatePasswordRequest;
import com.jpkocommunity.domain.user.dto.request.WithdrawRequest;
import com.jpkocommunity.domain.user.dto.response.MyCommentResponse;
import com.jpkocommunity.domain.user.dto.response.MyPostResponse;
import com.jpkocommunity.domain.user.dto.response.UserProfileResponse;
import com.jpkocommunity.domain.user.dto.response.UserPostResponse;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import com.jpkocommunity.global.util.CookieUtils;
import jakarta.servlet.http.HttpServletResponse;
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
    private final CookieUtils cookieUtils;
    private final ReportService reportService;

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody WithdrawRequest request,
            HttpServletResponse response
    ) {
        userService.withdraw(authUser.userId(), request);
        cookieUtils.delete(response, "accessToken");
        cookieUtils.delete(response, "refreshToken");
        return ResponseEntity.ok(ApiResponse.ok("회원 탈퇴가 완료되었습니다."));
    }

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

    @GetMapping("/by-nickname/{nickname}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getPublicUserProfile(
            @PathVariable String nickname
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserProfile(nickname)));
    }

    @GetMapping("/by-nickname/{nickname}/posts")
    public ResponseEntity<ApiResponse<Page<UserPostResponse>>> getPublicUserPosts(
            @PathVariable String nickname,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserPosts(nickname, pageable)));
    }

    @PatchMapping("/me/nickname")
    public ResponseEntity<ApiResponse<Void>> updateNickname(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody UpdateNicknameRequest request
    ) {
        userService.updateNickname(authUser.userId(), request);
        return ResponseEntity.ok(ApiResponse.ok("닉네임이 변경되었습니다."));
    }

    @PatchMapping("/me/bio")
    public ResponseEntity<ApiResponse<Void>> updateBio(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody UpdateBioRequest request
    ) {
        userService.updateBio(authUser.userId(), request);
        return ResponseEntity.ok(ApiResponse.ok("자기소개가 변경되었습니다."));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody UpdatePasswordRequest request
    ) {
        userService.updatePassword(authUser.userId(), request);
        return ResponseEntity.ok(ApiResponse.ok("비밀번호가 변경되었습니다."));
    }

    @GetMapping("/me/reports")
    public ResponseEntity<ApiResponse<Page<MyReportResponse>>> getMyReports(
            @AuthenticationPrincipal AuthUser authUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getMyReports(authUser.userId(), pageable)));
    }

}
