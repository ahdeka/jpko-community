package com.jpkocommunity.domain.point.controller;

import com.jpkocommunity.domain.point.dto.request.PachinkoPlayRequest;
import com.jpkocommunity.domain.point.dto.response.PachinkoConfigResponse;
import com.jpkocommunity.domain.point.dto.response.PachinkoPlayResponse;
import com.jpkocommunity.domain.point.service.PachinkoService;
import com.jpkocommunity.global.config.properties.PachinkoProperties;
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
@RequestMapping("/api/pachinko")
@RequiredArgsConstructor
public class PachinkoController {

    private final PachinkoService pachinkoService;
    private final PachinkoProperties pachinkoProperties;

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<PachinkoConfigResponse>> getConfig() {
        return ResponseEntity.ok(ApiResponse.ok(PachinkoConfigResponse.from(pachinkoProperties)));
    }

    @PostMapping("/play")
    public ResponseEntity<ApiResponse<PachinkoPlayResponse>> play(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PachinkoPlayRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(pachinkoService.play(authUser.userId(), request.ballCount())));
    }
}
