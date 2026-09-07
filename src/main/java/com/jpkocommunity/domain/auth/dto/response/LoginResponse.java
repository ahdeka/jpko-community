package com.jpkocommunity.domain.auth.dto.response;

import com.jpkocommunity.domain.point.dto.response.CheckInOutcome;
import com.jpkocommunity.domain.user.entity.UserRole;

public record LoginResponse(
        String nickname,
        UserRole role,
        CheckInInfo checkIn
) {

    // 로그인(및 회원가입 직후 자동 로그인) 시 출첵 결과. 오늘 이미 했으면 checkedIn=false
    public record CheckInInfo(boolean checkedIn, Long amount) {
        public static CheckInInfo from(CheckInOutcome outcome) {
            return new CheckInInfo(outcome.checkedIn(), outcome.amount());
        }
    }
}
