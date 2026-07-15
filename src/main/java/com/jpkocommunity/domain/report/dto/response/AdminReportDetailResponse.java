package com.jpkocommunity.domain.report.dto.response;

import com.jpkocommunity.domain.report.entity.Report;
import com.jpkocommunity.domain.report.entity.ReportReason;
import com.jpkocommunity.domain.report.entity.ReportStatus;
import com.jpkocommunity.domain.report.entity.ReportTargetType;

import java.time.LocalDateTime;

public record AdminReportDetailResponse(
        Long id,
        String reporterNickname,
        ReportReason reason,
        String detail,
        ReportStatus status,
        LocalDateTime createdAt
) {
    public static AdminReportDetailResponse from(Report report) {
        return new AdminReportDetailResponse(
                report.getId(),
                report.getReporter().getNickname(),
                report.getReason(),
                report.getDetail(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}