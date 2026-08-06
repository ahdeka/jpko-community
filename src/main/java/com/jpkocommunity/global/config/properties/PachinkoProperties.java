package com.jpkocommunity.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 파칭코(플린코형 구슬 낙하) 설정. rows개의 못 행을 지날 때마다 독립적으로 좌/우로 튕기고,
 * 우측으로 튕긴 횟수가 곧 최종 슬롯 인덱스가 된다(이항분포 -> 가운데 슬롯일수록 확률이 높음).
 */
@ConfigurationProperties(prefix = "app.pachinko")
public record PachinkoProperties(
        long betUnit,
        long minBet,
        long maxBet,
        double feeRate,
        int rows,
        double rightProbability,
        List<Double> slotMultipliers
) {
    public PachinkoProperties {
        if (betUnit <= 0) {
            throw new IllegalArgumentException("app.pachinko.bet-unit은 0보다 커야 합니다: " + betUnit);
        }
        if (minBet <= 0 || minBet % betUnit != 0) {
            throw new IllegalArgumentException(
                    "app.pachinko.min-bet(%d)는 bet-unit(%d)의 배수여야 합니다".formatted(minBet, betUnit));
        }
        if (maxBet % betUnit != 0) {
            throw new IllegalArgumentException(
                    "app.pachinko.max-bet(%d)는 bet-unit(%d)의 배수여야 합니다".formatted(maxBet, betUnit));
        }
        if (minBet > maxBet) {
            throw new IllegalArgumentException(
                    "app.pachinko.min-bet(%d)는 max-bet(%d)보다 클 수 없습니다".formatted(minBet, maxBet));
        }
        if (feeRate < 0 || feeRate > 1) {
            throw new IllegalArgumentException("app.pachinko.fee-rate는 0~1 사이여야 합니다: " + feeRate);
        }
        if (rows <= 0) {
            throw new IllegalArgumentException("app.pachinko.rows는 0보다 커야 합니다: " + rows);
        }
        if (rightProbability < 0 || rightProbability > 1) {
            throw new IllegalArgumentException(
                    "app.pachinko.right-probability는 0~1 사이여야 합니다: " + rightProbability);
        }
        if (slotMultipliers == null || slotMultipliers.size() != rows + 1) {
            throw new IllegalArgumentException(
                    "app.pachinko.slot-multipliers의 크기는 rows+1(%d)이어야 합니다".formatted(rows + 1));
        }
        for (Double m : slotMultipliers) {
            if (m == null || m < 0) {
                throw new IllegalArgumentException(
                        "app.pachinko.slot-multipliers는 모두 0 이상이어야 합니다: " + slotMultipliers);
            }
        }
    }

    // 베팅액이 유효한 범위·단위인지 검사
    public boolean isValidBet(long betAmount) {
        return betAmount >= minBet && betAmount <= maxBet && betAmount % betUnit == 0;
    }

    // 못 행을 rows번 통과시키며 매번 독립적으로 좌/우를 굴린다. 우측 횟수 = 최종 슬롯 인덱스.
    public RollResult roll() {
        List<Boolean> bounces = new ArrayList<>(rows);
        int slotIndex = 0;
        for (int i = 0; i < rows; i++) {
            boolean right = ThreadLocalRandom.current().nextDouble() < rightProbability;
            bounces.add(right);
            if (right) {
                slotIndex++;
            }
        }
        return new RollResult(List.copyOf(bounces), slotIndex, slotMultipliers.get(slotIndex));
    }

    // bounces: 애니메이션 재생을 위한 좌/우 순서 그대로. slotIndex: 착지한 슬롯(0..rows).
    public record RollResult(List<Boolean> bounces, int slotIndex, double multiplier) {}
}
