package com.jpkocommunity.domain.shop.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

// 현재/과거 시각을 넣으면 조기 종료(즉시 정산)로 처리된다.
public record UpdateRoleAuctionRoundRequest(
        @NotNull(message = "종료 시각을 입력해주세요.")
        LocalDateTime endAt
) {}
