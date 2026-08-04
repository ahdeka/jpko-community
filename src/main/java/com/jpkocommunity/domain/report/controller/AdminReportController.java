package com.jpkocommunity.domain.report.controller;

import com.jpkocommunity.domain.report.dto.request.AdminReportTargetStatusRequest;
import com.jpkocommunity.domain.report.dto.response.AdminReportDetailResponse;
import com.jpkocommunity.domain.report.dto.response.AdminReportSummaryResponse;
import com.jpkocommunity.domain.report.entity.ReportStatus;
import com.jpkocommunity.domain.report.entity.ReportTargetType;
import com.jpkocommunity.domain.report.service.ReportService;
import com.jpkocommunity.domain.user.entity.UserRole;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ROOT')")
public class AdminReportController {

    private final ReportService reportService;

    // ADMIN 대상(경매로 획득한 임시 ADMIN 포함) 신고는 ROOT만 볼 수 있다 —
    // 동료 ADMIN이 서로에 대한 신고를 감추거나 자기 신고를 스스로 반려하지 못하게 하려는 목적.
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Page<AdminReportSummaryResponse>>> getReports(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) ReportTargetType targetType,
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        boolean isRoot = authUser.role() == UserRole.ROOT;
        return ResponseEntity.ok(ApiResponse.ok(reportService.getReportSummaries(targetType, status, isRoot, pageable)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminReportDetailResponse>>> getDetails(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam ReportTargetType targetType,
            @RequestParam Long targetId
    ) {
        boolean isRoot = authUser.role() == UserRole.ROOT;
        return ResponseEntity.ok(ApiResponse.ok(reportService.getReportDetails(targetType, targetId, isRoot)));
    }

    @PatchMapping("/target-status")
    public ResponseEntity<ApiResponse<Void>> updateTargetStatus(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody AdminReportTargetStatusRequest request
    ) {
        boolean isRoot = authUser.role() == UserRole.ROOT;
        reportService.updateTargetStatus(request, isRoot);
        return ResponseEntity.ok(ApiResponse.ok("신고 처리 상태가 변경되었습니다."));
    }
}
