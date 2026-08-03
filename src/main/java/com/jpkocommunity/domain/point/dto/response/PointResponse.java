package com.jpkocommunity.domain.point.dto.response;

import com.jpkocommunity.domain.point.entity.Point;
import lombok.Builder;

@Builder
public record PointResponse(
        Long userId,
        Long balance
) {
    public static PointResponse from(Point point) {
        return new PointResponse(
                point.getUser() != null ? point.getUser().getId() : null,
                point.getBalance()
        );
    }
}