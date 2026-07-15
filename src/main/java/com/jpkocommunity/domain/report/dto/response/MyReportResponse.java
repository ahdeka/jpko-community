package com.jpkocommunity.domain.report.dto.response;

import com.jpkocommunity.domain.report.entity.ReportReason;
import com.jpkocommunity.domain.report.entity.ReportStatus;
import com.jpkocommunity.domain.report.entity.ReportTargetType;

import java.time.LocalDateTime;

public record MyReportResponse(
        Long id,
        ReportTargetType targetType,
        Long targetId,
        Long postId,
        String targetPreview,
        ReportReason reason,
        String detail,
        ReportStatus status,
        LocalDateTime createdAt
) {}