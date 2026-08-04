package com.jpkocommunity.domain.user.controller;

import com.jpkocommunity.domain.user.dto.request.UpdateUserGradeRequest;
import com.jpkocommunity.domain.user.dto.request.UpdateUserRoleRequest;
import com.jpkocommunity.domain.user.dto.request.UpdateUserStatusRequest;
import com.jpkocommunity.domain.user.dto.response.AdminUserResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getUsers(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUsers(keyword, pageable)));
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        userService.updateStatus(userId, request.status(), request.suspendedUntil(), authUser.userId());
        return ResponseEntity.ok(ApiResponse.ok("계정 상태가 변경되었습니다."));
    }

    @PatchMapping("/{userId}/grade")
    @PreAuthorize("hasAnyRole('ADMIN', 'ROOT')")
    public ResponseEntity<ApiResponse<Void>> updateGrade(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserGradeRequest request
    ) {
        userService.updateGrade(userId, request.grade());
        return ResponseEntity.ok(ApiResponse.ok("사용자 등급이 변경되었습니다."));
    }

    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('ROOT')")
    public ResponseEntity<ApiResponse<Void>> updateRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        userService.updateRole(userId, request.role());
        return ResponseEntity.ok(ApiResponse.ok("사용자 권한이 변경되었습니다."));
    }
}
