package com.jpkocommunity.domain.shop.controller;

import com.jpkocommunity.domain.shop.dto.response.RolePurchaseAdminResponse;
import com.jpkocommunity.domain.shop.service.RoleAuctionService;
import com.jpkocommunity.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 경매로 실제 ADMIN 권한을 보유 중인 유저 현황 — 임시 관리자 남용 감시용.
// ROOT 전용이다 — 감시 대상이 ADMIN 그 자체이므로 일반 ADMIN에게 보여주면 감시 의미가 없다.
@RestController
@RequestMapping("/api/admin/shop/role-purchases")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROOT')")
public class AdminRolePurchaseController {

    private final RoleAuctionService roleAuctionService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RolePurchaseAdminResponse>>> getActiveRolePurchases(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(roleAuctionService.getActiveRolePurchases(pageable)));
    }
}
