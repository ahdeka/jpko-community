package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.point.dto.response.MyRankingResponse;
import com.jpkocommunity.domain.point.dto.response.RankingEntryResponse;
import com.jpkocommunity.domain.point.dto.response.RankingRow;
import com.jpkocommunity.domain.point.dto.response.TopRankingResponse;
import com.jpkocommunity.domain.point.repository.PointRepository;
import com.jpkocommunity.global.config.properties.RankingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankingService {

    private final PointRepository pointRepository;
    private final RankingProperties rankingProperties;

    public TopRankingResponse getTopRankings() {
        return new TopRankingResponse(buildTopEntries());
    }

    public MyRankingResponse getMyRanking(Long userId) {
        long totalRanked = pointRepository.countRanked();
        List<RankingEntryResponse> topEntries = buildTopEntries();

        RankingRow myRow = pointRepository.findMyRankedEntry(userId)
                .orElse(null);

        // 랭킹 대상이 아니면 내 순위 없이 반환
        if (myRow == null) {
            return new MyRankingResponse(null, userId, totalRanked, topEntries, List.of());
        }

        int myRank = calculateRank(myRow.getBalance(), userId);
        int neighborSize = rankingProperties.neighborSize();

        List<RankingEntryResponse> neighbors = buildNeighbors(userId, myRow, myRank, neighborSize);

        return new MyRankingResponse(myRank, userId, totalRanked, topEntries, neighbors);
    }


    // ========== private 메서드 ==========

    private int calculateRank(Long myBalance, Long userId) {
        return (int) (pointRepository.countAbove(myBalance, userId) + 1);
    }

    private List<RankingEntryResponse> buildTopEntries() {
        List<RankingRow> rows = pointRepository.findTopRanking(rankingProperties.topSize());
        List<RankingEntryResponse> entries = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            entries.add(RankingEntryResponse.of(i + 1, rows.get(i)));
        }
        return entries;
    }

    private List<RankingEntryResponse> buildNeighbors(Long userId, RankingRow myRow, int myRank, int size) {
        long myBalance = myRow.getBalance();

        List<RankingEntryResponse> neighbors = new ArrayList<>();
        neighbors.addAll(rankedAbove(myBalance, userId, myRank, size));
        neighbors.add(RankingEntryResponse.of(myRank, myRow));
        neighbors.addAll(rankedBelow(myBalance, userId, myRank, size));
        return neighbors;
    }

    private List<RankingEntryResponse> rankedAbove(long myBalance, long userId, int myRank, int size) {
        List<RankingRow> above = pointRepository.findAbove(myBalance, userId, size);

        List<RankingEntryResponse> result = new ArrayList<>(above.size());
        for (int i = above.size() - 1; i >= 0; i--) {
            int rank = myRank - (i + 1);
            result.add(RankingEntryResponse.of(rank, above.get(i)));
        }
        return result;
    }

    private List<RankingEntryResponse> rankedBelow(long myBalance, long userId, int myRank, int size) {
        List<RankingRow> below = pointRepository.findBelow(myBalance, userId, size);
        List<RankingEntryResponse> result = new ArrayList<>(below.size());
        for (int i = 0; i < below.size(); i++) {
            result.add(RankingEntryResponse.of(myRank + 1 + i, below.get(i)));
        }
        return result;
    }

}
