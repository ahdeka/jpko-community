package com.jpkocommunity.domain.like.dto.request;

import com.jpkocommunity.domain.like.entity.LikeType;
import jakarta.validation.constraints.NotNull;

public record LikeRequest(

        @NotNull(message = "추천/비추천을 선택해주세요.")
        LikeType type
) {}