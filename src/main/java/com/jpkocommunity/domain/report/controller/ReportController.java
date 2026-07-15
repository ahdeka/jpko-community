package com.jpkocommunity.domain.report.controller;

import com.jpkocommunity.domain.report.dto.request.ReportCreateRequest;
import com.jpkocommunity.domain.report.dto.response.ReportResponse;
import com.jpkocommunity.domain.report.service.ReportService;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody ReportCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("신고가 접수되었습니다.",
                        reportService.createReport(authUser.userId(), request)));
    }

}
