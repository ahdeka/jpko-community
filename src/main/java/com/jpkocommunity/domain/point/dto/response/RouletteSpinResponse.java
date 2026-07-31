package com.jpkocommunity.domain.point.dto.response;

// 룰렛 한 판의 결과
public record RouletteSpinResponse(
        boolean win,
        long betAmount,
        long fee,
        long resultAmount,      // 실제로 오간 금액(상대 잔액 부족 시 배팅액보다 작을 수 있음)
        long balanceAfter,
        Long opponentId,
        String opponentNickname,
        boolean opponentLocked
) {}
