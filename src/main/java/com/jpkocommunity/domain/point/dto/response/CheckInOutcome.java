package com.jpkocommunity.domain.point.dto.response;

// 매일 로그인 출첵 결과
public record CheckInOutcome(boolean checkedIn, Long amount) {

    public static CheckInOutcome checkedIn(long amount) {
        return new CheckInOutcome(true, amount);
    }

    // 오늘 이미 출첵했거나, 로그인/회원가입 흐름이 아니라 출첵 대상이 아닌 경우 모두 결과 형태는 동일함
    public static CheckInOutcome none() {
        return new CheckInOutcome(false, null);
    }
}
