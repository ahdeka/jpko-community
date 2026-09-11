package com.jpkocommunity.domain.point.dto.response;

import com.jpkocommunity.global.config.properties.SlotProperties;

import java.util.List;

public record SlotConfigResponse(
        long betUnit,
        long minBet,
        long maxBet,
        double feeRate,
        List<SymbolInfo> symbols
) {
    public record SymbolInfo(String name, double multiplier, double tripleProbability) {}

    public static SlotConfigResponse from(SlotProperties properties) {
        double totalWeight = properties.symbols().stream().mapToDouble(SlotProperties.Symbol::weight).sum();
        return new SlotConfigResponse(
                properties.betUnit(), properties.minBet(), properties.maxBet(), properties.feeRate(),
                properties.symbols().stream()
                        .map(s -> new SymbolInfo(s.name(), s.multiplier(), Math.pow(s.weight() / totalWeight, 3)))
                        .toList());
    }
}
