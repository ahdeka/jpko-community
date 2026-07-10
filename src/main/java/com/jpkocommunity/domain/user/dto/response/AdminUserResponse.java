package com.jpkocommunity.domain.user.dto.response;

import com.jpkocommunity.domain.comment.entity.Comment;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserGrade;
import com.jpkocommunity.domain.user.entity.UserRole;
import com.jpkocommunity.domain.user.entity.UserStatus;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String email,
        String nickname,
        UserRole role,
        UserStatus status,
        UserGrade grade,
        String displayGradeName,
        boolean emailVerified,
        LocalDateTime createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(), user.getEmail(), user.getNickname(),
                user.getRole(), user.getStatus(), user.getGrade(),
                user.getGrade().getDisplayGradeName(),
                user.isEmailVerified(), user.getCreatedAt()
        );
    }
}