package com.jpkocommunity.domain.report.dto.request;

import com.jpkocommunity.domain.report.entity.ReportReason;
import com.jpkocommunity.domain.report.entity.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest(
        @NotNull(message = "신고 대상 종류를 선택해주세요.")
        ReportTargetType targetType,

        @NotNull(message = "신고 대상을 선택해주세요.")
        Long targetId,

        @NotNull(message = "신고 사유를 선택해주세요.")
        ReportReason reason,

        // ETC 선택 시에만 필수 — 검증은 서비스 단에서 처리
        @Size(max = 500, message = "상세 사유는 500자 이내로 입력해주세요.")
        String detail
) {}