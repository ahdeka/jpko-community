package com.jpkocommunity.domain.user.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateBioRequest(
        @Size(max = 200, message = "자기소개는 200자 이하여야 합니다.")
        String bio
) {
}
