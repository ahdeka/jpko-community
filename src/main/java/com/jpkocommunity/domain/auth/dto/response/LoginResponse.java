package com.jpkocommunity.domain.auth.dto.response;

import com.jpkocommunity.domain.point.dto.response.RouletteOutcome;
import com.jpkocommunity.domain.user.entity.UserRole;

public record LoginResponse(String nickname, UserRole role, RouletteInfo roulette) {

    // 로그인 시 일일 포인트 룰렛 결과. 회원가입 등 룰렛 대상이 아닌 경로에서는 spun=false
    public record RouletteInfo(boolean spun, Long amount) {
        public static RouletteInfo from(RouletteOutcome outcome) {
            return new RouletteInfo(outcome.spun(), outcome.amount());
        }
    }
}