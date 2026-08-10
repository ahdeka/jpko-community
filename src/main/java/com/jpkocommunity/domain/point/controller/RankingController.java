package com.jpkocommunity.domain.point.controller;

import com.jpkocommunity.domain.point.dto.response.MyRankingResponse;
import com.jpkocommunity.domain.point.dto.response.TopRankingResponse;
import com.jpkocommunity.domain.point.service.RankingService;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/points/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    // 상위 랭킹 조회
    @GetMapping("/top")
    public ResponseEntity<ApiResponse<TopRankingResponse>> getTopRanking(
    ) {
        return ResponseEntity.ok(ApiResponse.ok(rankingService.getTopRankings()));
    }

    // 내 순위 + 주변 (인증 필수)
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyRankingResponse>> getMyRanking(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(rankingService.getMyRanking(authUser.userId())));
    }
}
