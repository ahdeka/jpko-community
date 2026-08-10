package com.jpkocommunity.domain.point.dto.response;

import java.util.List;

public record MyRankingResponse(
        Integer myRank,
        Long myUserId,
        long totalRanked,
        List<RankingEntryResponse> topEntries,
        List<RankingEntryResponse> neighbors
) {}