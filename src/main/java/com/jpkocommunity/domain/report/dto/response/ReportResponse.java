package com.jpkocommunity.domain.report.dto.response;

public record ReportResponse(Long id) {
    public static ReportResponse from(Long id) {
        return new ReportResponse(id);
    }
}