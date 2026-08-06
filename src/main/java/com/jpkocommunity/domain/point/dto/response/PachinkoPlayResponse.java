package com.jpkocommunity.domain.point.dto.response;

import java.util.List;

// 파칭코 한 판의 결과. 구슬 ballCount개를 각각 독립적으로 쏘고 그 결과를 합산한다.
public record PachinkoPlayResponse(
        int ballCount,
        long ballPrice,
        long totalCost,           // ballCount * ballPrice
        long fee,
        List<PachinkoBallResult> balls,   // 구슬 개수만큼, 쏜 순서 그대로
        long totalPayout,
        long netAmount,            // totalPayout - totalCost (음수면 순손실)
        boolean win,               // netAmount > 0
        long balanceAfter
) {}
