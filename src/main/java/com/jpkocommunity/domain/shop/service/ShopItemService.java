package com.jpkocommunity.domain.shop.service;

import com.jpkocommunity.domain.shop.dto.request.CreateShopItemRequest;
import com.jpkocommunity.domain.shop.dto.request.UpdateShopItemRequest;
import com.jpkocommunity.domain.shop.dto.response.ShopItemResponse;
import com.jpkocommunity.domain.shop.entity.ShopItem;
import com.jpkocommunity.domain.shop.entity.ShopItemType;
import com.jpkocommunity.domain.shop.repository.ShopItemRepository;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 상점 상품 카탈로그 조회 + 관리(ROOT 전용). 가격/수량 조정 권한을 ADMIN이 아닌 ROOT로 제한하는 이유는
// AdminShopItemController의 @PreAuthorize("hasRole('ROOT')")에서 강제한다 (경매로 얻은 ADMIN이
// 상점 가격·수량을 스스로 조작하는 자기강화 루프를 막기 위함).
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShopItemService {

    private final ShopItemRepository shopItemRepository;
    private final RoleAuctionService roleAuctionService;

    public List<ShopItemResponse> getActiveItems() {
        return shopItemRepository.findByActiveTrue().stream().map(ShopItemResponse::from).toList();
    }

    // ========== ROOT 전용 ==========

    public List<ShopItemResponse> getAllItems() {
        return shopItemRepository.findAll().stream().map(ShopItemResponse::from).toList();
    }

    @Transactional
    public void createItem(CreateShopItemRequest request) {
        ShopItem item = switch (request.type()) {
            case BLOCK -> {
                requireNonNull(request.price(), request.durationMinutes());
                yield ShopItem.createBlockItem(request.name(), request.price(), request.durationMinutes());
            }
            case ROLE_AUCTION -> {
                requireNonNull(request.targetRole(), request.quantity(), request.minPrice(),
                        request.durationDays(), request.cadenceDays());
                yield ShopItem.createRoleAuctionItem(request.name(), request.targetRole(), request.quantity(),
                        request.minPrice(), request.durationDays(), request.cadenceDays());
            }
        };
        shopItemRepository.save(item);

        // ROLE_AUCTION은 등록 즉시(active=true 기본) 첫 회차를 연다 — 스케줄러 tick(최대 5분)을 기다리지 않음.
        if (item.getType() == ShopItemType.ROLE_AUCTION) {
            roleAuctionService.openRoundIfNeeded(item);
        }
    }

    @Transactional
    public void updateItem(Long itemId, UpdateShopItemRequest request) {
        ShopItem item = shopItemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));

        if (item.getType() == ShopItemType.BLOCK) {
            item.updateBlockPricing(
                    request.price() != null ? request.price() : item.getPrice(),
                    request.durationMinutes() != null ? request.durationMinutes() : item.getDurationMinutes()
            );
        } else {
            item.updateRoleAuctionConfig(
                    request.quantity() != null ? request.quantity() : item.getQuantity(),
                    request.minPrice() != null ? request.minPrice() : item.getMinPrice(),
                    request.durationDays() != null ? request.durationDays() : item.getDurationDays(),
                    request.cadenceDays() != null ? request.cadenceDays() : item.getCadenceDays()
            );
        }

        if (request.active() != null) {
            boolean wasInactive = !item.isActive();
            if (request.active()) {
                item.activate();
                // 비활성 -> 활성으로 막 전환된 ROLE_AUCTION만 등록 때와 동일하게 즉시 회차를 연다.
                // 이미 활성 상태인 아이템에 (프론트가 현재 값을 그대로 다시 보내는 식으로) active:true가
                // 반복해서 오는 경우까지 매번 호출할 필요는 없다(openRoundIfNeeded 자체는 멱등이라
                // 실질적 피해는 없었지만, cadenceDays로 대기 중이던 다음 회차가 의도치 않게 앞당겨질 수 있었음).
                if (wasInactive && item.getType() == ShopItemType.ROLE_AUCTION) {
                    roleAuctionService.openRoundIfNeeded(item);
                }
            } else {
                item.deactivate();
            }
        }
    }

    private void requireNonNull(Object... values) {
        for (Object value : values) {
            if (value == null) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
        }
    }
}
