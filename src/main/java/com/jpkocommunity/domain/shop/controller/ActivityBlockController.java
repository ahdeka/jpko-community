package com.jpkocommunity.domain.shop.controller;

import com.jpkocommunity.domain.shop.dto.request.BlockAppealRequest;
import com.jpkocommunity.domain.shop.dto.request.PurchaseBlockRequest;
import com.jpkocommunity.domain.shop.dto.response.BlockStatusResponse;
import com.jpkocommunity.domain.shop.service.ActivityBlockService;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop/blocks")
@RequiredArgsConstructor
public class ActivityBlockController {

    private final ActivityBlockService activityBlockService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> purchaseBlock(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PurchaseBlockRequest request
    ) {
        activityBlockService.purchaseBlock(authUser.userId(), request);
        return ResponseEntity.ok(ApiResponse.ok("차단권을 사용했습니다."));
    }

    // 본인이 차단된 상태에서도 호출 가능 (BlockAllowedPaths에 등록됨)
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<BlockStatusResponse>> getMyBlockStatus(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(activityBlockService.getMyBlockStatus(authUser.userId())));
    }

    // 본인이 차단된 상태에서도 호출 가능 (BlockAllowedPaths에 등록됨)
    @PostMapping("/me/unblock")
    public ResponseEntity<ApiResponse<Void>> unblockSelf(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        activityBlockService.unblockSelf(authUser.userId());
        return ResponseEntity.ok(ApiResponse.ok("차단이 해제되었습니다."));
    }

    // 본인이 차단된 상태에서도 호출 가능 (BlockAllowedPaths에 등록됨)
    @PostMapping("/me/appeal")
    public ResponseEntity<ApiResponse<Void>> appeal(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody BlockAppealRequest request
    ) {
        activityBlockService.appeal(authUser.userId(), request);
        return ResponseEntity.ok(ApiResponse.ok("관리자에게 메시지를 보냈습니다."));
    }
}
