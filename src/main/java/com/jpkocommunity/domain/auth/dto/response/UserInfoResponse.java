package com.jpkocommunity.domain.auth.dto.response;

import com.jpkocommunity.domain.level.dto.response.UserLevelResponse;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserRole;

import java.time.LocalDateTime;

public record UserInfoResponse(
        Long id,
        String email,
        String nickname,
        UserRole role,
        String bio,
        boolean emailVerified,
        LocalDateTime nicknameChangeableAt,
        UserLevelResponse level
) {
    public static UserInfoResponse from(User user, long exp) {
        return new UserInfoResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getBio(),
                user.isEmailVerified(),
                user.getNicknameChangeableAt(),
                UserLevelResponse.of(exp)
        );
    }
}
