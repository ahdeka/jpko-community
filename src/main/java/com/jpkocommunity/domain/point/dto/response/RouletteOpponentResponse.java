package com.jpkocommunity.domain.point.dto.response;

// 현재 고정된 룰렛 상대. 고정된 상대가 없으면 컨트롤러가 이 자체를 null로 응답한다.
public record RouletteOpponentResponse(Long opponentId, String opponentNickname) {}
