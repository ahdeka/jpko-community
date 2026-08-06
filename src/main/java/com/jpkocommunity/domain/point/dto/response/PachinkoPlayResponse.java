package com.jpkocommunity.domain.point.dto.response;

import java.util.List;

// 파칭코 한 판의 결과. bounces는 프론트가 구슬 낙하 애니메이션을 그릴 수 있도록 좌/우 순서 그대로 전달한다.
public record PachinkoPlayResponse(
        long betAmount,
        long fee,
        List<Boolean> bounces,   // true = 오른쪽으로 튕김
        int slotIndex,
        double multiplier,
        long payout,             // round(betAmount * multiplier)
        long netAmount,          // payout - betAmount (음수면 순손실)
        boolean win,              // netAmount > 0
        long balanceAfter
) {}
