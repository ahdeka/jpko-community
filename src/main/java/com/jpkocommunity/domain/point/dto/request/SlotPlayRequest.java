package com.jpkocommunity.domain.point.dto.request;

import jakarta.validation.constraints.NotNull;

public record SlotPlayRequest(
        @NotNull(message = "배팅액을 입력해주세요.")
        Long betAmount
) {}
