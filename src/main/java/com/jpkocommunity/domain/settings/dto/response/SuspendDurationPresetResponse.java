package com.jpkocommunity.domain.settings.dto.response;

import com.jpkocommunity.domain.settings.entity.SuspendDurationPreset;
import lombok.Builder;

@Builder
public record SuspendDurationPresetResponse(
        Long id,
        String label,
        Long minutes,
        Integer displayOrder
) {
    public static SuspendDurationPresetResponse from(SuspendDurationPreset preset) {
        return SuspendDurationPresetResponse.builder()
                .id(preset.getId())
                .label(preset.getLabel())
                .minutes(preset.getMinutes())
                .displayOrder(preset.getDisplayOrder())
                .build();
    }
}
