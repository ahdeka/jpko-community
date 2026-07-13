package com.jpkocommunity.domain.auth.dto.response;

import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserGrade;
import com.jpkocommunity.domain.user.entity.UserRole;

public record UserInfoResponse(
        Long id,
        String email,
        String nickname,
        UserRole role,
        boolean emailVerified,
        UserGrade grade
) {
    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.isEmailVerified(),
                user.getGrade()
        );
    }
}
