package com.jpkocommunity.domain.point.controller;

import com.jpkocommunity.domain.point.dto.request.SlotPlayRequest;
import com.jpkocommunity.domain.point.dto.response.SlotConfigResponse;
import com.jpkocommunity.domain.point.dto.response.SlotPlayResponse;
import com.jpkocommunity.domain.point.service.SlotService;
import com.jpkocommunity.global.config.properties.SlotProperties;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/slot")
@RequiredArgsConstructor
public class SlotController {

    private final SlotService slotService;
    private final SlotProperties slotProperties;

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<SlotConfigResponse>> getConfig() {
        return ResponseEntity.ok(ApiResponse.ok(SlotConfigResponse.from(slotProperties)));
    }

    @PostMapping("/play")
    public ResponseEntity<ApiResponse<SlotPlayResponse>> play(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody SlotPlayRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(slotService.play(authUser.userId(), request.betAmount())));
    }
}
