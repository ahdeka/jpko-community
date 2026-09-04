package com.jpkocommunity.domain.user.dto.response;

import com.jpkocommunity.domain.level.service.LevelPolicy;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserRole;
import com.jpkocommunity.domain.user.entity.UserStatus;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String nickname,
        int level,
        boolean adminAuthor,
        boolean rootAuthor,
        boolean suspended,
        String bio,
        LocalDateTime createdAt
) {
    public static UserProfileResponse from(User user, long exp) {
        return new UserProfileResponse(
                user.getId(),
                user.getNickname(),
                LevelPolicy.levelOfExp(exp),
                user.getRole() == UserRole.ADMIN,
                user.getRole() == UserRole.ROOT,
                user.getStatus() == UserStatus.SUSPENDED,
                user.getBio(),
                user.getCreatedAt()
        );
    }
}