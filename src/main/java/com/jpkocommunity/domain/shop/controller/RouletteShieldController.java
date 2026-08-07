package com.jpkocommunity.domain.shop.controller;

import com.jpkocommunity.domain.shop.dto.request.PurchaseShieldRequest;
import com.jpkocommunity.domain.shop.dto.response.RouletteShieldStatusResponse;
import com.jpkocommunity.domain.shop.service.RouletteShieldService;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop/shields")
@RequiredArgsConstructor
public class RouletteShieldController {

    private final RouletteShieldService rouletteShieldService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> purchaseShield(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PurchaseShieldRequest request
    ) {
        rouletteShieldService.purchaseShield(authUser.userId(), request.itemId());
        return ResponseEntity.ok(ApiResponse.ok("방어권을 구매했습니다."));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<RouletteShieldStatusResponse>> getMyShieldStatus(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(rouletteShieldService.getMyShieldStatus(authUser.userId())));
    }
}
