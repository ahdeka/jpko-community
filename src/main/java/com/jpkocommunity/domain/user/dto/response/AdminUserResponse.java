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
        LocalDateTime createdAt,
        // 포인트로 산 차단권(#6 ActivityBlock)에 의해 현재 활동이 제한 중인지. status(계정 정지)와는
        // 완전히 별개 메커니즘이라 이 필드가 없으면 관리자 목록에서 차단된 유저가 "정상"으로 보였다.
        // null이면 차단 중이 아니고, 값이 있으면 그 시각까지 차단 중이다.
        LocalDateTime activeBlockExpiresAt
) {
    public static AdminUserResponse from(User user, LocalDateTime activeBlockExpiresAt) {
        return new AdminUserResponse(
                user.getId(), user.getEmail(), user.getNickname(),
                user.getRole(), user.getStatus(), user.getGrade(),
                user.getGrade().getDisplayGradeName(),
                user.isEmailVerified(), user.getCreatedAt(),
                activeBlockExpiresAt
        );
    }
}