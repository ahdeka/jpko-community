package com.jpkocommunity.domain.shop.dto.response;

import com.jpkocommunity.domain.shop.entity.BlockAppeal;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record BlockAppealResponse(
        Long id,
        Long activityBlockId,
        String senderNickname,
        String message,
        boolean handled,
        LocalDateTime createdAt
) {
    public static BlockAppealResponse from(BlockAppeal appeal) {
        return BlockAppealResponse.builder()
                .id(appeal.getId())
                .activityBlockId(appeal.getBlock().getId())
                .senderNickname(appeal.getSender().getDisplayNickname())
                .message(appeal.getMessage())
                .handled(appeal.isHandled())
                .createdAt(appeal.getCreatedAt())
                .build();
    }
}
