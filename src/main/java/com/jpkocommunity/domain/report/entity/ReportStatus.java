package com.jpkocommunity.domain.report.entity;

public enum ReportStatus {
    PENDING,   // 접수됨, 처리 대기
    RESOLVED,  // 조치 완료(대상 삭제 등)
    REJECTED   // 허위/근거 없음으로 반려
}
