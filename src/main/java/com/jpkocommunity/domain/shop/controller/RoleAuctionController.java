package com.jpkocommunity.domain.shop.controller;

import com.jpkocommunity.domain.shop.dto.request.PlaceBidRequest;
import com.jpkocommunity.domain.shop.dto.response.RoleAuctionRoundResponse;
import com.jpkocommunity.domain.shop.dto.response.RolePurchaseStatusResponse;
import com.jpkocommunity.domain.shop.service.RoleAuctionService;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shop/role-auctions")
@RequiredArgsConstructor
public class RoleAuctionController {

    private final RoleAuctionService roleAuctionService;

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<List<RoleAuctionRoundResponse>>> getCurrentRounds() {
        return ResponseEntity.ok(ApiResponse.ok(roleAuctionService.getCurrentRounds()));
    }

    @PostMapping("/{roundId}/bids")
    public ResponseEntity<ApiResponse<Void>> placeBid(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long roundId,
            @Valid @RequestBody PlaceBidRequest request
    ) {
        roleAuctionService.placeBid(authUser.userId(), roundId, request);
        return ResponseEntity.ok(ApiResponse.ok("입찰이 완료되었습니다."));
    }

    @DeleteMapping("/{roundId}/bids")
    public ResponseEntity<ApiResponse<Void>> cancelBid(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long roundId
    ) {
        roleAuctionService.cancelBid(authUser.userId(), roundId);
        return ResponseEntity.ok(ApiResponse.ok("입찰을 취소했습니다."));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<RolePurchaseStatusResponse>> getMyRoleStatus(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(roleAuctionService.getMyRoleStatus(authUser.userId())));
    }
}
