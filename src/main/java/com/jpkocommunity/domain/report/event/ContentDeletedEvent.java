package com.jpkocommunity.domain.report.event;

import com.jpkocommunity.domain.report.entity.ReportTargetType;

// 신고 대상이 삭제될 때 발생하는 이벤트
public record ContentDeletedEvent(ReportTargetType targetType, Long targetId) {}
