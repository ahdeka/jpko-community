package com.jpkocommunity.domain.point.dto.response;

import com.jpkocommunity.domain.user.entity.User;
import lombok.Builder;

@Builder
public record PointResponse (
    Long point,
    User user
) {

}
