package com.jpkocommunity.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 3릴 슬롯머신 설정. 심볼마다 등장 가중치(weight)와 3개 모두 같을 때의 배당(multiplier)을 갖는다.
 * 3개가 모두 같아야만 배당이 있고(2/3 부분 배당 없음), 아니면 배당은 0이다.
 */
@ConfigurationProperties(prefix = "app.slot")
public record SlotProperties(
        long betUnit,
        long minBet,
        long maxBet,
        double feeRate,
        List<Symbol> symbols
) {
    public SlotProperties {
        if (betUnit <= 0) {
            throw new IllegalArgumentException("app.slot.bet-unit은 0보다 커야 합니다: " + betUnit);
        }
        if (minBet <= 0 || minBet % betUnit != 0) {
            throw new IllegalArgumentException(
                    "app.slot.min-bet(%d)는 bet-unit(%d)의 배수여야 합니다".formatted(minBet, betUnit));
        }
        if (maxBet % betUnit != 0) {
            throw new IllegalArgumentException(
                    "app.slot.max-bet(%d)는 bet-unit(%d)의 배수여야 합니다".formatted(maxBet, betUnit));
        }
        if (minBet > maxBet) {
            throw new IllegalArgumentException(
                    "app.slot.min-bet(%d)는 max-bet(%d)보다 클 수 없습니다".formatted(minBet, maxBet));
        }
        if (feeRate < 0 || feeRate > 1) {
            throw new IllegalArgumentException("app.slot.fee-rate는 0~1 사이여야 합니다: " + feeRate);
        }
        if (symbols == null || symbols.isEmpty()) {
            throw new IllegalArgumentException("app.slot.symbols는 최소 1개 이상이어야 합니다.");
        }
        for (Symbol s : symbols) {
            if (s.name() == null || s.name().isBlank()) {
                throw new IllegalArgumentException("app.slot.symbols의 name은 비어있을 수 없습니다.");
            }
            if (s.weight() <= 0) {
                throw new IllegalArgumentException("app.slot.symbols의 weight는 0보다 커야 합니다: " + s.weight());
            }
            if (s.multiplier() < 0) {
                throw new IllegalArgumentException("app.slot.symbols의 multiplier는 0 이상이어야 합니다: " + s.multiplier());
            }
        }
    }

    public record Symbol(String name, double weight, double multiplier) {}

    // 베팅액이 유효한 범위·단위인지 검사
    public boolean isValidBet(long betAmount) {
        return betAmount >= minBet && betAmount <= maxBet && betAmount % betUnit == 0;
    }

    // 3개 릴을 독립적으로 굴린 뒤, 셋 다 같은 심볼일 때만 그 심볼의 multiplier를 배당으로 준다.
    public RollResult roll() {
        Symbol s1 = rollSymbol();
        Symbol s2 = rollSymbol();
        Symbol s3 = rollSymbol();
        boolean win = s1.name().equals(s2.name()) && s2.name().equals(s3.name());
        double multiplier = win ? s1.multiplier() : 0.0;
        return new RollResult(List.of(s1.name(), s2.name(), s3.name()), win, multiplier);
    }

    // CheckInProperties.rollAmount()와 동일한 누적가중치 기법을 심볼 선택에 맞게 적용한 것.
    private Symbol rollSymbol() {
        double totalWeight = symbols.stream().mapToDouble(Symbol::weight).sum();
        double roll = ThreadLocalRandom.current().nextDouble(0, totalWeight);
        double cumulative = 0;
        for (Symbol s : symbols) {
            cumulative += s.weight();
            if (roll < cumulative) {
                return s;
            }
        }
        return symbols.get(symbols.size() - 1); // 부동소수점 오차 폴백
    }

    // symbols: 릴에 나온 3개 심볼 이름(애니메이션용). win/multiplier: 트리플 매치 여부와 그 배당.
    public record RollResult(List<String> symbols, boolean win, double multiplier) {}
}
