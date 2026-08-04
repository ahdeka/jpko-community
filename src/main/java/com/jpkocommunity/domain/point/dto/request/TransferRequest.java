package com.jpkocommunity.domain.point.dto.request;

import jakarta.validation.constraints.NotNull;

public record TransferRequest(
        @NotNull(message = "수취인을 입력해주세요.")
        Long recipientId,
        @NotNull(message = "포인트를 입력해주세요.")
        Long amount
) {
}
