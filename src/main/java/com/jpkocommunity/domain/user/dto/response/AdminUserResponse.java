package com.jpkocommunity.domain.user.dto.response;

import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserRole;
import com.jpkocommunity.domain.user.entity.UserStatus;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String email,
        String nickname,
        UserRole role,
        UserStatus status,
        boolean emailVerified,
        LocalDateTime createdAt,
        // 정지 중일 때의 해제 예정 시각. null이면 정지가 아니거나 ROOT가 건 무기한 정지다
        // (구분은 status로 한다).
        LocalDateTime suspendedUntil
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(), user.getEmail(), user.getNickname(),
                user.getRole(), user.getStatus(),
                user.isEmailVerified(), user.getCreatedAt(),
                user.getSuspendedUntil()
        );
    }
}