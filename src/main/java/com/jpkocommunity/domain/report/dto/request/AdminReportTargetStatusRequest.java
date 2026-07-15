package com.jpkocommunity.domain.report.dto.request;

import com.jpkocommunity.domain.report.entity.ReportStatus;
import com.jpkocommunity.domain.report.entity.ReportTargetType;
import jakarta.validation.constraints.NotNull;

// 관리자가 신고 처리 상태 변경 시 사용하는 DTO
public record AdminReportTargetStatusRequest(
        @NotNull ReportTargetType targetType,
        @NotNull Long targetId,
        @NotNull(message = "처리 상태를 선택해주세요.")
        ReportStatus status  // RESOLVED 또는 REJECTED만 허용
) {}