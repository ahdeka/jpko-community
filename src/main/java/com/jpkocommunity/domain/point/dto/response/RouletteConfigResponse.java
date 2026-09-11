package com.jpkocommunity.domain.point.dto.response;

import com.jpkocommunity.global.config.properties.RouletteProperties;

public record RouletteConfigResponse(
        long betUnit,
        long minBet,
        long maxBet,
        double feeRate,
        double winProbability,
        boolean agreed
) {
    public static RouletteConfigResponse from(RouletteProperties properties, boolean agreed) {
        return new RouletteConfigResponse(
                properties.betUnit(), properties.minBet(), properties.maxBet(), properties.feeRate(),
                properties.winProbability(), agreed);
    }
}
