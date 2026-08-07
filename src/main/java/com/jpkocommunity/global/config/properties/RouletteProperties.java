package com.jpkocommunity.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 룰렛(베팅형 대결) 설정
 */
@ConfigurationProperties(prefix = "app.roulette")
public record RouletteProperties(
        long betUnit,
        long minBet,
        long maxBet,
        double winProbability,
        double feeRate,
        String secretKey
) {
    public RouletteProperties {
        if (betUnit <= 0) {
            throw new IllegalArgumentException("app.roulette.bet-unit은 0보다 커야 합니다: " + betUnit);
        }
        if (minBet <= 0 || minBet % betUnit != 0) {
            throw new IllegalArgumentException(
                    "app.roulette.min-bet(%d)는 bet-unit(%d)의 배수여야 합니다".formatted(minBet, betUnit));
        }
        if (maxBet % betUnit != 0) {
            throw new IllegalArgumentException(
                    "app.roulette.max-bet(%d)는 bet-unit(%d)의 배수여야 합니다".formatted(maxBet, betUnit));
        }
        if (minBet > maxBet) {
            throw new IllegalArgumentException(
                    "app.roulette.min-bet(%d)는 max-bet(%d)보다 클 수 없습니다".formatted(minBet, maxBet));
        }
        if (winProbability < 0 || winProbability > 1) {
            throw new IllegalArgumentException("app.roulette.win-probability는 0~1 사이여야 합니다: " + winProbability);
        }
        if (feeRate < 0 || feeRate > 1) {
            throw new IllegalArgumentException("app.roulette.fee-rate는 0~1 사이여야 합니다: " + feeRate);
        }
    }

    // 베팅액이 유효한 범위·단위인지 검사
    public boolean isValidBet(long betAmount) {
        return betAmount >= minBet && betAmount <= maxBet && betAmount % betUnit == 0;
    }
}
