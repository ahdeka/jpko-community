package com.jpkocommunity.domain.settings.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateSuspendDurationPresetRequest(
        @NotNull(message = "기한(분)을 입력해주세요.")
        @Positive(message = "기한은 0보다 커야 합니다.")
        Long minutes
) {}
