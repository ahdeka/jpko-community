package com.jpkocommunity.domain.point.dto.response;

import java.util.List;

// 구슬 1개의 결과. bounces는 프론트가 그 구슬의 낙하 애니메이션을 그릴 수 있도록 좌/우 순서 그대로 전달한다.
public record PachinkoBallResult(
        List<Boolean> bounces,   // true = 오른쪽으로 튕김
        int slotIndex,
        double multiplier,
        long payout               // round(ballPrice * multiplier)
) {}
