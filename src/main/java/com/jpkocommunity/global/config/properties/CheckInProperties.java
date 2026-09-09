package com.jpkocommunity.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 매일 로그인 출첵 설정
 * 그 달의 누적 출석 횟수가 출석 마일스톤에 도달하면 보너스가 붙도록
 */
@ConfigurationProperties(prefix = "app.checkin")
public record CheckInProperties(
        long amount,
        List<MonthlyBonus> monthlyBonuses
) {

    public record MonthlyBonus(int count, long bonus) {}

    public CheckInProperties {
        monthlyBonuses = monthlyBonuses == null
                ? List.of()
                : List.copyOf(monthlyBonuses);
        if (amount <= 0) {
            throw new IllegalArgumentException("app.checkin.amount는 0보다 커야 합니다: " + amount);
        }
        for (MonthlyBonus b : monthlyBonuses) {
            // 2월을 위해서
            if (b.count() < 1 || b.count() >= 28) {
                throw new IllegalArgumentException(
                        "app.checkin.monthly-bonuses의 count는 1~27이어야 합니다: " + b.count());
            }
            if (b.bonus() < 0) {
                throw new IllegalArgumentException(
                        "app.checkin.monthly-bonuses의 bonus는 음수일 수 없습니다: " + b.bonus());
            }
        }
    }

    // 이 달 n번째 출석에 붙는 보너스
    public long bonusOf(int monthlyCount) {
        return monthlyBonuses.stream()
                .filter(m -> m.count() == monthlyCount)
                .mapToLong(MonthlyBonus::bonus)
                .findFirst()
                .orElse(0L);
    }
}
