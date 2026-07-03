package com.jpkocommunity.domain.auth.dto.response;

import com.jpkocommunity.domain.user.entity.UserRole;

public record UserInfoResponse(Long id, String email, String nickname, UserRole role, boolean emailVerified) {}
