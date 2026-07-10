package com.jpkocommunity.domain.user.dto.request;

import com.jpkocommunity.domain.user.entity.UserGrade;
import com.jpkocommunity.domain.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserGradeRequest(
        @NotNull(message = "등급을 선택해주세요.")
        UserGrade grade
) {}
