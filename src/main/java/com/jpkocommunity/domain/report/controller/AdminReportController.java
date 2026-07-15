package com.jpkocommunity.domain.report.controller;

import com.jpkocommunity.domain.report.dto.request.AdminReportTargetStatusRequest;
import com.jpkocommunity.domain.report.dto.response.AdminReportDetailResponse;
import com.jpkocommunity.domain.report.dto.response.AdminReportSummaryResponse;
import com.jpkocommunity.domain.report.entity.ReportStatus;
import com.jpkocommunity.domain.report.entity.ReportTargetType;
import com.jpkocommunity.domain.report.service.ReportService;
import com.jpkocommunity.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Page<AdminReportSummaryResponse>>> getReports(
            @RequestParam(required = false) ReportTargetType targetType,
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getReportSummaries(targetType, status, pageable)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminReportDetailResponse>>> getDetails(
            @RequestParam ReportTargetType targetType,
            @RequestParam Long targetId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getReportDetails(targetType, targetId)));
    }

    @PatchMapping("/target-status")
    public ResponseEntity<ApiResponse<Void>> updateTargetStatus(
            @Valid @RequestBody AdminReportTargetStatusRequest request
    ) {
        reportService.updateTargetStatus(request);
        return ResponseEntity.ok(ApiResponse.ok("신고 처리 상태가 변경되었습니다."));
    }
}
