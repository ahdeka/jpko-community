package com.jpkocommunity.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 매일 로그인 출첵 설정. 지급액은 여러 구간(range)으로 나뉘고, 구간마다 가중치(확률)가 다르다.
 * 각 구간 안에서는 unit 단위로만 균등 랜덤 지급된다(예: unit=50이면 -300, -250, ... 500처럼 50의 배수만 나옴).
 */
@ConfigurationProperties(prefix = "app.checkin")
public record CheckInProperties(long unit, List<Range> ranges) {

    public CheckInProperties {
        if (unit <= 0) {
            throw new IllegalArgumentException("app.checkin.unit은 0보다 커야 합니다: " + unit);
        }
        if (ranges == null || ranges.isEmpty()) {
            throw new IllegalArgumentException("app.checkin.ranges는 최소 1개 이상이어야 합니다.");
        }
        for (Range r : ranges) {
            if (r.minAmount() > r.maxAmount()) {
                throw new IllegalArgumentException(
                        "app.checkin.ranges의 min-amount(%d)는 max-amount(%d)보다 클 수 없습니다"
                                .formatted(r.minAmount(), r.maxAmount()));
            }
            if (r.minAmount() % unit != 0 || r.maxAmount() % unit != 0) {
                throw new IllegalArgumentException(
                        "app.checkin.ranges의 min-amount/max-amount는 unit(%d)의 배수여야 합니다: [%d, %d]"
                                .formatted(unit, r.minAmount(), r.maxAmount()));
            }
            if (r.weight() <= 0) {
                throw new IllegalArgumentException("app.checkin.ranges의 weight는 0보다 커야 합니다: " + r.weight());
            }
        }
    }

    public record Range(long minAmount, long maxAmount, double weight) {}

    // 구간을 가중치(weight) 비례로 하나 고른 뒤, 그 구간 안에서 unit 단위로 균등 랜덤 금액을 뽑는다.
    public long rollAmount() {
        double totalWeight = ranges.stream().mapToDouble(Range::weight).sum();
        double roll = ThreadLocalRandom.current().nextDouble(0, totalWeight);

        double cumulative = 0;
        for (Range r : ranges) {
            cumulative += r.weight();
            if (roll < cumulative) {
                return rollWithinRange(r);
            }
        }
        return rollWithinRange(ranges.get(ranges.size() - 1)); // 부동소수점 오차로 못 걸렸을 때의 폴백
    }

    private long rollWithinRange(Range r) {
        long steps = (r.maxAmount() - r.minAmount()) / unit;
        return r.minAmount() + unit * ThreadLocalRandom.current().nextLong(0, steps + 1);
    }
}
