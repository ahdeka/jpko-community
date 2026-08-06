package com.jpkocommunity.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 파칭코(플린코형 구슬 낙하) 설정. 구슬 1개당 가격(ballPrice)을 정해두고 유저가 개수를 골라 여러 발을
 * 쏜다. 구슬 1개는 rows개의 못 행을 지날 때마다 독립적으로 좌/우로 튕기고, 우측으로 튕긴 횟수가 곧
 * 최종 슬롯 인덱스가 된다(이항분포 -> 가운데 슬롯일수록 확률이 높음). 여러 발을 합산하는 책임은
 * PachinkoService에 있다 — 이 클래스는 구슬 1개의 결과만 산출한다.
 */
@ConfigurationProperties(prefix = "app.pachinko")
public record PachinkoProperties(
        long ballPrice,
        int minBalls,
        int maxBalls,
        double feeRate,
        int rows,
        double rightProbability,
        List<Double> slotMultipliers
) {
    public PachinkoProperties {
        if (ballPrice <= 0) {
            throw new IllegalArgumentException("app.pachinko.ball-price는 0보다 커야 합니다: " + ballPrice);
        }
        if (minBalls < 1) {
            throw new IllegalArgumentException("app.pachinko.min-balls는 1 이상이어야 합니다: " + minBalls);
        }
        if (maxBalls < minBalls) {
            throw new IllegalArgumentException(
                    "app.pachinko.max-balls(%d)는 min-balls(%d)보다 작을 수 없습니다".formatted(maxBalls, minBalls));
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

    // 구슬 개수가 유효한 범위인지 검사
    public boolean isValidBallCount(int ballCount) {
        return ballCount >= minBalls && ballCount <= maxBalls;
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
