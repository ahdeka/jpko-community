package com.jpkocommunity.domain.point.dto.response;

import com.jpkocommunity.global.config.properties.RouletteProperties;

// 룰렛 베팅 제약. 승률은 의도적으로 노출하지 않는다.
public record RouletteConfigResponse(long betUnit, long minBet, long maxBet, double feeRate, boolean agreed) {
    public static RouletteConfigResponse from(RouletteProperties properties, boolean agreed) {
        return new RouletteConfigResponse(
                properties.betUnit(), properties.minBet(), properties.maxBet(), properties.feeRate(), agreed);
    }
}
