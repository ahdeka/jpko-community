package com.jpkocommunity.domain.shop.controller;

import com.jpkocommunity.domain.shop.dto.request.UpdateRoleAuctionRoundRequest;
import com.jpkocommunity.domain.shop.service.RoleAuctionService;
import com.jpkocommunity.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 진행 중인 경매 회차의 종료 시각 직접 수정/조기 종료 — ROOT 전용.
// 상품(shop_item) 설정 관리와 마찬가지로 ADMIN에게는 허용하지 않는다(자기강화 루프 방지 원칙과 동일).
@RestController
@RequestMapping("/api/admin/shop/role-auctions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROOT')")
public class AdminRoleAuctionController {

    private final RoleAuctionService roleAuctionService;

    @PatchMapping("/{roundId}")
    public ResponseEntity<ApiResponse<Void>> updateRoundEndAt(
            @PathVariable Long roundId,
            @Valid @RequestBody UpdateRoleAuctionRoundRequest request
    ) {
        roleAuctionService.updateRoundEndAt(roundId, request.endAt());
        return ResponseEntity.ok(ApiResponse.ok("회차 종료 시각이 변경되었습니다."));
    }
}
