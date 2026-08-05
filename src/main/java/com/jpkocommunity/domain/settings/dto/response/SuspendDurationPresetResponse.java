package com.jpkocommunity.domain.settings.dto.response;

import com.jpkocommunity.domain.settings.entity.SuspendDurationPreset;
import lombok.Builder;

@Builder
public record SuspendDurationPresetResponse(
        Long id,
        Long minutes
) {
    public static SuspendDurationPresetResponse from(SuspendDurationPreset preset) {
        return SuspendDurationPresetResponse.builder()
                .id(preset.getId())
                .minutes(preset.getMinutes())
                .build();
    }
}
