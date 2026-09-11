package com.jpkocommunity.domain.point.dto.response;

import com.jpkocommunity.global.config.properties.PachinkoProperties;

import java.util.ArrayList;
import java.util.List;

public record PachinkoConfigResponse(
        long ballPrice,
        int minBalls,
        int maxBalls,
        double feeRate,
        int rows,
        List<Double> slotMultipliers,
        List<Double> slotProbabilities,
        double rightProbability
) {
    public static PachinkoConfigResponse from(PachinkoProperties properties) {
        return new PachinkoConfigResponse(
                properties.ballPrice(), properties.minBalls(), properties.maxBalls(),
                properties.feeRate(), properties.rows(), properties.slotMultipliers(),
                slotProbabilities(properties.rows(), properties.rightProbability()),
                properties.rightProbability());
    }

    private static List<Double> slotProbabilities(int rows, double rightProbability) {
        List<Double> probabilities = new ArrayList<>(rows + 1);
        double coefficient = 1;
        for (int k = 0; k <= rows; k++) {
            probabilities.add(coefficient
                    * Math.pow(rightProbability, k)
                    * Math.pow(1 - rightProbability, rows - k));
            coefficient = coefficient * (rows - k) / (k + 1);
        }
        return List.copyOf(probabilities);
    }
}
