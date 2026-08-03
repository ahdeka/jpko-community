package com.jpkocommunity.domain.point.dto;

import com.jpkocommunity.domain.point.entity.Point;
import com.jpkocommunity.domain.user.entity.User;

public record LockedAccount(Long userId, Point point, User user) {
    boolean isSystem() {
        return userId == null;
    }
}
