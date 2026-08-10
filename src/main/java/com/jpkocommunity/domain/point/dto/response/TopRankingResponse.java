package com.jpkocommunity.domain.point.dto.response;

import java.util.List;

public record TopRankingResponse(
        List<RankingEntryResponse> entries
) {}