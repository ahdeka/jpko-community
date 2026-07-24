package com.jpkocommunity.domain.user.dto.response;

import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserGrade;
import com.jpkocommunity.domain.user.entity.UserRole;
import com.jpkocommunity.domain.user.entity.UserStatus;

import java.time.LocalDateTime;

public record PublicProfileResponse(
        Long id,
        String nickname,
        UserGrade grade,
        String displayGradeName,
        boolean adminAuthor,
        boolean suspended,
        String bio,
        LocalDateTime createdAt
) {
    public static PublicProfileResponse from(User user) {
        return new PublicProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getGrade(),
                user.getGrade().getDisplayGradeName(),
                user.getRole() == UserRole.ADMIN,
                user.getStatus() == UserStatus.SUSPENDED,
                user.getBio(),
                user.getCreatedAt()
        );
    }
}