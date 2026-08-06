package com.jpkocommunity.domain.point.dto.response;

import java.util.List;

// 슬롯머신 한 판의 결과. symbols는 프론트가 릴을 그대로 렌더링할 수 있도록 3개 심볼 이름을 순서대로 담는다.
public record SlotPlayResponse(
        long betAmount,
        long fee,
        List<String> symbols,
        boolean tripleMatch,
        double multiplier,
        long payout,
        long netAmount,
        boolean win,       // netAmount > 0
        long balanceAfter
) {}
