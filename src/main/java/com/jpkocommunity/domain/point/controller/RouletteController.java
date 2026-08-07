package com.jpkocommunity.domain.point.controller;

import com.jpkocommunity.domain.point.dto.request.RouletteSpinRequest;
import com.jpkocommunity.domain.point.dto.response.RouletteConfigResponse;
import com.jpkocommunity.domain.point.dto.response.RouletteOpponentResponse;
import com.jpkocommunity.domain.point.dto.response.RouletteSpinResponse;
import com.jpkocommunity.domain.point.service.RouletteService;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.config.properties.RouletteProperties;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roulette")
@RequiredArgsConstructor
public class RouletteController {

    private final RouletteService rouletteService;
    private final RouletteProperties rouletteProperties;
    private final UserService userService;

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<RouletteConfigResponse>> getConfig(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        boolean agreed = userService.findById(authUser.userId()).isRouletteStatus();
        return ResponseEntity.ok(ApiResponse.ok(RouletteConfigResponse.from(rouletteProperties, agreed)));
    }

    @PostMapping("/consent")
    public ResponseEntity<ApiResponse<Void>> agreeToRoulette(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        rouletteService.agreeToRoulette(authUser.userId());
        return ResponseEntity.ok(ApiResponse.ok("룰렛 참여에 동의했습니다."));
    }

    @GetMapping("/opponent")
    public ResponseEntity<ApiResponse<RouletteOpponentResponse>> getOpponent(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(rouletteService.getOpponent(authUser.userId(), null).orElse(null)));
    }

    @PutMapping("/opponent/{opponentId}")
    public ResponseEntity<ApiResponse<Void>> lockOpponent(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long opponentId
    ) {
        rouletteService.lockOpponent(authUser.userId(), opponentId, null);
        return ResponseEntity.ok(ApiResponse.ok("상대가 고정되었습니다."));
    }

    @DeleteMapping("/opponent")
    public ResponseEntity<ApiResponse<Void>> unlockOpponent(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        rouletteService.unlockOpponent(authUser.userId(), null);
        return ResponseEntity.ok(ApiResponse.ok("고정이 해제되었습니다."));
    }

    @PostMapping("/spin")
    public ResponseEntity<ApiResponse<RouletteSpinResponse>> spin(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody RouletteSpinRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(rouletteService.spin(authUser.userId(), request.betAmount())));
    }
}
