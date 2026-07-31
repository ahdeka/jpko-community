package com.jpkocommunity.domain.point.dto.response;

// 로그인 시 일일 포인트 룰렛 결과
public record RouletteOutcome(boolean spun, Long amount) {

    public static RouletteOutcome spun(long amount) {
        return new RouletteOutcome(true, amount);
    }

    // 오늘 이미 돌렸거나(alreadyPlayed), 피해자 후보가 없었거나(noVictimAvailable),
    // 로그인 흐름이 아니라 룰렛 대상이 아닌 경우(none) 모두 결과 형태는 동일함
    public static RouletteOutcome none() {
        return new RouletteOutcome(false, null);
    }
}
