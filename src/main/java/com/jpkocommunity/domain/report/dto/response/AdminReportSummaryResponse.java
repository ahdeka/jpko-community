package com.jpkocommunity.domain.report.dto.response;

import com.jpkocommunity.domain.report.entity.ReportReason;
import com.jpkocommunity.domain.report.entity.ReportStatus;
import com.jpkocommunity.domain.report.entity.ReportTargetType;

import java.time.LocalDateTime;

public record AdminReportSummaryResponse(
        ReportTargetType targetType,
        Long targetId,
        String targetPreview,
        String targetAuthor,
        long reportCount,
        LocalDateTime lastReportedAt
) {}