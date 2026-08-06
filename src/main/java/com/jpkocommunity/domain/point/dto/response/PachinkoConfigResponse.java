package com.jpkocommunity.domain.point.dto.response;

import com.jpkocommunity.global.config.properties.PachinkoProperties;

import java.util.List;

// 파칭코 구슬 가격/개수 제약 + 슬롯별 배당표. 못의 좌/우 확률(right-probability)은 의도적으로 노출하지 않는다.
public record PachinkoConfigResponse(
        long ballPrice,
        int minBalls,
        int maxBalls,
        double feeRate,
        int rows,
        List<Double> slotMultipliers
) {
    public static PachinkoConfigResponse from(PachinkoProperties properties) {
        return new PachinkoConfigResponse(
                properties.ballPrice(), properties.minBalls(), properties.maxBalls(),
                properties.feeRate(), properties.rows(), properties.slotMultipliers());
    }
}
