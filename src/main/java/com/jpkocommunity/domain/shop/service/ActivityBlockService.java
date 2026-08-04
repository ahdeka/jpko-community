package com.jpkocommunity.domain.shop.service;

import com.jpkocommunity.domain.point.entity.TransactionType;
import com.jpkocommunity.domain.point.service.PointService;
import com.jpkocommunity.domain.shop.dto.request.BlockAppealRequest;
import com.jpkocommunity.domain.shop.dto.request.PurchaseBlockRequest;
import com.jpkocommunity.domain.shop.dto.response.BlockStatusResponse;
import com.jpkocommunity.domain.shop.entity.ActivityBlock;
import com.jpkocommunity.domain.shop.entity.BlockAppeal;
import com.jpkocommunity.domain.shop.entity.BlockStatus;
import com.jpkocommunity.domain.shop.entity.ShopItem;
import com.jpkocommunity.domain.shop.entity.ShopItemType;
import com.jpkocommunity.domain.shop.repository.ActivityBlockRepository;
import com.jpkocommunity.domain.shop.repository.BlockAppealRepository;
import com.jpkocommunity.domain.shop.repository.ShopItemRepository;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserRole;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.domain.shop.dto.response.BlockAppealResponse;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 임시 활동 차단(#6): 구매/조기해제/이의제기/내 차단 상태 조회를 한 곳에서 처리.
// 로그인 게이팅 자체는 JwtAuthenticationFilter가 ActivityBlockRepository를 직접 조회해서 수행한다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityBlockService {

    private final ActivityBlockRepository activityBlockRepository;
    private final BlockAppealRepository blockAppealRepository;
    private final ShopItemRepository shopItemRepository;
    private final UserService userService;
    private final PointService pointService;

    @Transactional
    public void purchaseBlock(Long buyerId, PurchaseBlockRequest request) {
        if (buyerId.equals(request.targetUserId())) {
            throw new CustomException(ErrorCode.SELF_BLOCK_NOT_ALLOWED);
        }

        User target = userService.findById(request.targetUserId());
        if (target.getRole() != UserRole.USER) {
            throw new CustomException(ErrorCode.TARGET_NOT_BLOCKABLE);
        }

        ShopItem item = findActiveBlockItem(request.itemId());
        User blocker = userService.findById(buyerId);

        pointService.charge(buyerId, item.getPrice(), TransactionType.BLOCK_PURCHASE,
                "활동 차단권 구매 - 대상: " + target.getId());

        activityBlockRepository.findActiveByTargetForUpdate(target)
                .ifPresentOrElse(
                        existing -> existing.extend(blocker, item, item.getPrice()),
                        () -> activityBlockRepository.save(ActivityBlock.create(target, blocker, item, item.getPrice()))
                );
    }

    @Transactional
    public void unblockSelf(Long userId) {
        User user = userService.findById(userId);
        ActivityBlock block = activityBlockRepository.findActiveByTargetForUpdate(user)
                .orElseThrow(() -> new CustomException(ErrorCode.BLOCK_NOT_FOUND));

        pointService.charge(userId, block.getPrice(), TransactionType.BLOCK_UNBLOCK, "활동 차단 조기 해제");
        block.lift();
    }

    @Transactional
    public void appeal(Long userId, BlockAppealRequest request) {
        User user = userService.findById(userId);
        ActivityBlock block = activityBlockRepository.findByTargetAndStatus(user, BlockStatus.ACTIVE)
                .orElseThrow(() -> new CustomException(ErrorCode.BLOCK_NOT_FOUND));

        blockAppealRepository.save(BlockAppeal.create(block, user, request.message()));
    }

    public BlockStatusResponse getMyBlockStatus(Long userId) {
        User user = userService.findById(userId);
        return activityBlockRepository.findByTargetAndStatus(user, BlockStatus.ACTIVE)
                .filter(ActivityBlock::isCurrentlyActive)
                .map(BlockStatusResponse::from)
                .orElseGet(BlockStatusResponse::notBlocked);
    }

    // ========== 관리자 기능 ==========

    public Page<BlockAppealResponse> getAppeals(boolean handled, Pageable pageable) {
        return blockAppealRepository.findByHandled(handled, pageable).map(BlockAppealResponse::from);
    }

    @Transactional
    public void markAppealHandled(Long appealId) {
        BlockAppeal appeal = blockAppealRepository.findById(appealId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        appeal.markHandled();
    }

    // ========== private 메서드 ==========

    private ShopItem findActiveBlockItem(Long itemId) {
        ShopItem item = shopItemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));
        if (item.getType() != ShopItemType.BLOCK) {
            throw new CustomException(ErrorCode.ITEM_NOT_FOUND);
        }
        if (!item.isActive()) {
            throw new CustomException(ErrorCode.ITEM_INACTIVE);
        }
        return item;
    }
}
