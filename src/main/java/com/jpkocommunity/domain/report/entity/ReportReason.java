package com.jpkocommunity.domain.report.entity;

public enum ReportReason {
    SPAM,      // 스팸, 광고
    ABUSE,     // 욕설,혐오,선정적 표현 등 부적절한 콘텐츠
    ILLEGAL,   // 불법 정보, 개인정보 노출, 사기 등
    ETC        // 기타 - detail 텍스트 필수
}
