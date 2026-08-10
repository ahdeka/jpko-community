package com.jpkocommunity.domain.point.dto.response;

import lombok.Builder;

@Builder
public record RankingEntryResponse(
        int rank,
        Long userId,
        String nickname,
        Long balance
) {
    public static RankingEntryResponse of(int rank, RankingRow row) {
        return RankingEntryResponse.builder()
                .rank(rank)
                .userId(row.getUserId())
                .nickname(row.getNickname())
                .balance(row.getBalance())
                .build();
    }
}
