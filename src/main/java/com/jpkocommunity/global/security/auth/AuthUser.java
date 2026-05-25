package com.jpkocommunity.global.security.auth;

import com.jpkocommunity.domain.user.entity.UserRole;

public record AuthUser(Long userId, UserRole role) {
}