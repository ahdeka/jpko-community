package com.jpkocommunity.domain.settings.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateSuspendDurationPresetRequest(
        @NotBlank(message = "이름을 입력해주세요.")
        String label,

        @NotNull(message = "기한(분)을 입력해주세요.")
        @Positive(message = "기한은 0보다 커야 합니다.")
        Long minutes,

        @NotNull(message = "순서를 입력해주세요.")
        Integer displayOrder
) {}
