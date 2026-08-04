package com.jpkocommunity.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@ConfigurationProperties(prefix = "app.transfer")
public record TransferProperties(long unit, List<Range> feeRanges) {

    public TransferProperties {
        if (unit <= 0) {
            throw new IllegalArgumentException("app.transfer.unit은 0보다 커야 합니다: " + unit);
        }
        if (feeRanges == null || feeRanges.isEmpty()) {
            throw new IllegalArgumentException("app.transfer.ranges는 최소 1개 이상이어야 합니다.");
        }
        for (Range r : feeRanges) {
            if (r.minAmount() > r.maxAmount()) {
                throw new IllegalArgumentException(
                        "app.transfer.ranges의 min-amount(%d)는 max-amount(%d)보다 클 수 없습니다"
                                .formatted(r.minAmount(), r.maxAmount()));
            }
            if (r.minAmount() % unit != 0 || r.maxAmount() % unit != 0) {
                throw new IllegalArgumentException(
                        "app.transfer.ranges의 min-amount/max-amount는 unit(%d)의 배수여야 합니다: [%d, %d]"
                                .formatted(unit, r.minAmount(), r.maxAmount()));
            }
            if (r.weight() <= 0) {
                throw new IllegalArgumentException("app.transfer.ranges의 weight는 0보다 커야 합니다: " + r.weight());
            }
        }
    }

    public record Range(long minAmount, long maxAmount, double weight) {}

    public long rollAmount() {
        double totalWeight = feeRanges.stream().mapToDouble(Range::weight).sum();
        double roll = ThreadLocalRandom.current().nextDouble(0, totalWeight);

        double cumulative = 0;
        for (Range r : feeRanges) {
            cumulative += r.weight();
            if (roll < cumulative) {
                return rollWithinRange(r);
            }
        }
        return rollWithinRange(feeRanges.get(feeRanges.size() - 1)); // 부동소수점 오차로 못 걸렸을 때의 폴백
    }

    private long rollWithinRange(Range r) {
        long steps = (r.maxAmount() - r.minAmount()) / unit;
        return r.minAmount() + unit * ThreadLocalRandom.current().nextLong(0, steps + 1);
    }
}


