package com.jpkocommunity.domain.settings.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateSuspendUnlockPriceRequest(
        @NotNull(message = "가격을 입력해주세요.")
        @Positive(message = "가격은 0보다 커야 합니다.")
        Long price
) {}
