package com.jpkocommunity.domain.checkin.dto.response;

// 매일 로그인 출첵 결과
public record CheckInOutcome(
        long amount, // 기본 + 보너스 합계
        long bonusAmount, // 출석 보너스만
        int monthlyCount,
        long expAmount
) {
}
