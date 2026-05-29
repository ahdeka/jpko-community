package com.jpkocommunity.domain.like.dto.response;

import com.jpkocommunity.domain.like.entity.LikeType;

public record LikeResponse(
        long likeCount,
        long dislikeCount,
        LikeType myType   // 현재 유저가 누른 타입, 취소 시 null
) {}