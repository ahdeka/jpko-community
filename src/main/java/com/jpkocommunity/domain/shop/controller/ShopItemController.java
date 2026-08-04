package com.jpkocommunity.domain.shop.controller;

import com.jpkocommunity.domain.shop.dto.response.ShopItemResponse;
import com.jpkocommunity.domain.shop.service.ShopItemService;
import com.jpkocommunity.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shop/items")
@RequiredArgsConstructor
public class ShopItemController {

    private final ShopItemService shopItemService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShopItemResponse>>> getActiveItems() {
        return ResponseEntity.ok(ApiResponse.ok(shopItemService.getActiveItems()));
    }
}
