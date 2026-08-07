package com.jpkocommunity.domain.shop.service;

import com.jpkocommunity.domain.point.entity.TransactionType;
import com.jpkocommunity.domain.point.service.PointService;
import com.jpkocommunity.domain.shop.dto.response.RouletteShieldStatusResponse;
import com.jpkocommunity.domain.shop.entity.RouletteShield;
import com.jpkocommunity.domain.shop.entity.RouletteShieldStatus;
import com.jpkocommunity.domain.shop.entity.ShopItem;
import com.jpkocommunity.domain.shop.entity.ShopItemType;
import com.jpkocommunity.domain.shop.repository.RouletteShieldRepository;
import com.jpkocommunity.domain.shop.repository.ShopItemRepository;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 룰렛 방어권(#SHIELD): 구매하면 즉시 룰렛 동의를 취소해(cancelRouletteAgreement) 대결 대상에서
// 빠지고, 기간이 끝나면 구매 시점의 동의 상태로 원복한다. RoleAuctionService.grantRole/
// expireRolePurchases와 동일한 "임시로 내 계정 상태를 바꿨다가 스케줄러가 원복" 패턴이다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouletteShieldService {

    private final RouletteShieldRepository rouletteShieldRepository;
    private final ShopItemRepository shopItemRepository;
    private final UserService userService;
    private final PointService pointService;

    @Transactional
    public void purchaseShield(Long buyerId, Long itemId) {
        ShopItem item = findActiveShieldItem(itemId);
        User buyer = userService.findById(buyerId);

        pointService.charge(buyerId, item.getPrice(), TransactionType.SHIELD_PURCHASE, "룰렛 방어권 구매");

        // activity_blocks.extend와 같은 이유로 isCurrentlyActive()까지 확인한다 — 스케줄러가 아직
        // EXPIRED로 못 바꾼 실질적으로 끝난 row 위에 새 기간을 얹지 않기 위함.
        Optional<RouletteShield> existing = rouletteShieldRepository.findActiveByUserForUpdate(buyer);
        if (existing.isPresent() && existing.get().isCurrentlyActive()) {
            existing.get().extend(item, item.getPrice());
        } else {
            existing.ifPresent(RouletteShield::expire);
            rouletteShieldRepository.save(
                    RouletteShield.create(buyer, item, item.getPrice(), buyer.isRouletteStatus()));
        }

        buyer.cancelRouletteAgreement();
    }

    public RouletteShieldStatusResponse getMyShieldStatus(Long userId) {
        User user = userService.findById(userId);
        return rouletteShieldRepository.findByUserAndStatus(user, RouletteShieldStatus.ACTIVE)
                .filter(RouletteShield::isCurrentlyActive)
                .map(RouletteShieldStatusResponse::from)
                .orElseGet(RouletteShieldStatusResponse::inactive);
    }

    // ========== 스케줄러 ==========

    // RoleAuctionService.expireRolePurchases()와 동일한 이유로 벌크 UPDATE가 아니라 건별 처리 —
    // 만료 시 previousRouletteStatus가 true였던 유저만 user.agreeToRoulette()로 되돌려야 하는
    // 비즈니스 로직(단순 status 플립이 아님)이 각 row마다 붙기 때문.
    @Transactional
    public void expireShields() {
        List<RouletteShield> expired = rouletteShieldRepository.findAllExpired(LocalDateTime.now());
        for (RouletteShield shield : expired) {
            shield.expire();
            // 방어권이 걸려있던 동안 유저가 스스로 다시 동의했을 수 있다 - 이미 true면 건드리지 않는다.
            if (shield.isPreviousRouletteStatus() && !shield.getUser().isRouletteStatus()) {
                shield.getUser().agreeToRoulette();
            }
        }
    }

    // ========== private 메서드 ==========

    private ShopItem findActiveShieldItem(Long itemId) {
        ShopItem item = shopItemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));
        if (item.getType() != ShopItemType.SHIELD) {
            throw new CustomException(ErrorCode.ITEM_NOT_FOUND);
        }
        if (!item.isActive()) {
            throw new CustomException(ErrorCode.ITEM_INACTIVE);
        }
        return item;
    }
}
