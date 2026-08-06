package com.jpkocommunity.domain.point.dto.response;

import com.jpkocommunity.global.config.properties.SlotProperties;

import java.util.List;

// 슬롯머신 베팅 제약 + 심볼별 트리플 배당표. 각 심볼의 등장 가중치(weight)는 의도적으로 노출하지 않는다.
public record SlotConfigResponse(
        long betUnit,
        long minBet,
        long maxBet,
        double feeRate,
        List<SymbolInfo> symbols
) {
    public record SymbolInfo(String name, double multiplier) {}

    public static SlotConfigResponse from(SlotProperties properties) {
        return new SlotConfigResponse(
                properties.betUnit(), properties.minBet(), properties.maxBet(), properties.feeRate(),
                properties.symbols().stream()
                        .map(s -> new SymbolInfo(s.name(), s.multiplier()))
                        .toList());
    }
}
