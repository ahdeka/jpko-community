package com.jpkocommunity.domain.checkin.dto.response;

import java.util.List;

/**
 * 출석 페이지용 월별 조회
 */
public record CheckInCalendarResponse(
        int year,
        int month,
        int daysInMonth,
        List<Integer> checkedInDays, // 출석한 '일'만 (예: [1, 2, 5, 7])
        int monthlyCount,
        boolean checkedInToday, // 출석체크 버튼 활성화용
        List<Milestone> milestones
) {

    public record Milestone(int count, long bonus, boolean achieved) {}
}