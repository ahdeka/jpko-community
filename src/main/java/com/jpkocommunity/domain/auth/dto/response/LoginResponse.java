package com.jpkocommunity.domain.auth.dto.response;

import com.jpkocommunity.domain.user.entity.UserRole;

public record LoginResponse(String nickname, UserRole role) {}