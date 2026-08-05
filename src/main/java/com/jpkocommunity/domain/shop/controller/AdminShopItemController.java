package com.jpkocommunity.domain.shop.controller;

import com.jpkocommunity.domain.shop.dto.request.CreateShopItemRequest;
import com.jpkocommunity.domain.shop.dto.request.UpdateShopItemRequest;
import com.jpkocommunity.domain.shop.dto.response.ShopItemResponse;
import com.jpkocommunity.domain.shop.service.ShopItemService;
import com.jpkocommunity.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 가격/수량/판매중지 등 상점 카탈로그 관리는 ROOT 전용.
// hasAnyRole('ADMIN','ROOT')를 쓰지 않는 게 핵심 — 경매로 ROLE_ADMIN을 얻은 유저가
// 상점 가격·수량을 스스로 조작하는 자기강화 루프를 막기 위함.
@RestController
@RequestMapping("/api/admin/shop/items")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROOT')")
public class AdminShopItemController {

    private final ShopItemService shopItemService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShopItemResponse>>> getAllItems() {
        return ResponseEntity.ok(ApiResponse.ok(shopItemService.getAllItems()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createItem(@Valid @RequestBody CreateShopItemRequest request) {
        shopItemService.createItem(request);
        return ResponseEntity.ok(ApiResponse.ok("상품이 등록되었습니다."));
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<ApiResponse<Void>> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateShopItemRequest request
    ) {
        shopItemService.updateItem(itemId, request);
        return ResponseEntity.ok(ApiResponse.ok("상품이 수정되었습니다."));
    }
}
