package com.jpkocommunity.domain.point.dto.request;

import jakarta.validation.constraints.NotNull;

public record PachinkoPlayRequest(
        @NotNull(message = "구슬 개수를 입력해주세요.")
        Integer ballCount
) {}
