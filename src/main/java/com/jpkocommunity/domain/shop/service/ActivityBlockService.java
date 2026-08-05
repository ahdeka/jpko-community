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

import java.util.Optional;

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

        // status만 보면 스케줄러(최대 10분 주기)가 아직 EXPIRED로 못 바꾼, 실질적으로 이미 끝난 row를
        // 살아있는 것으로 오인해 그 만료 시각 위에 새 기간을 얹어버릴 수 있다 - isCurrentlyActive()로
        // 실제 만료 여부를 재확인한다.
        Optional<ActivityBlock> existing = activityBlockRepository.findActiveByTargetForUpdate(target);
        if (existing.isPresent() && existing.get().isCurrentlyActive()) {
            existing.get().extend(blocker, item, item.getPrice());
        } else {
            // 타깃당 ACTIVE row는 최대 1개(부분 유니크 인덱스)라, 새로 만들기 전에 실질적으로
            // 끝난 기존 row를 먼저 EXPIRED로 전환해야 한다.
            existing.ifPresent(ActivityBlock::expire);
            activityBlockRepository.save(ActivityBlock.create(target, blocker, item, item.getPrice()));
        }
    }

    // purchaseBlock()은 포인트 계좌 락 -> ActivityBlock row 락 순서로 잠근다. 여기서도 같은 순서를
    // 지켜야 한다(반대 순서면 "누군가 대상을 재차단 구매"와 "본인이 셀프 해제"가 거의 동시에 겹칠 때
    // 서로 다른 순서로 락을 걸어 데드락이 날 수 있다). 그래서 청구할 가격만 락 없이 먼저 읽고,
    // 포인트를 먼저 잠근 뒤에 ActivityBlock row를 잠근다. 단, 그 사이(락 없이 가격을 읽은 시점과
    // 실제로 row를 잠그는 시점) 다른 사람이 재차단/연장을 커밋해 가격이 바뀌었을 수 있으므로,
    // 락을 잡은 뒤 가격이 처음 청구한 값과 같은지 반드시 재검증한다 - 다르면 그대로 lift()하지 않고
    // 방금 청구한 금액을 돌려준 뒤 실패로 처리해 재시도를 유도한다.
    @Transactional
    public void unblockSelf(Long userId) {
        User user = userService.findById(userId);
        Long previewPrice = activityBlockRepository.findByTargetAndStatus(user, BlockStatus.ACTIVE)
                .filter(ActivityBlock::isCurrentlyActive)
                .orElseThrow(() -> new CustomException(ErrorCode.BLOCK_NOT_FOUND))
                .getPrice();

        pointService.charge(userId, previewPrice, TransactionType.BLOCK_UNBLOCK, "활동 차단 조기 해제");

        ActivityBlock block = activityBlockRepository.findActiveByTargetForUpdate(user)
                .filter(ActivityBlock::isCurrentlyActive)
                .orElseThrow(() -> new CustomException(ErrorCode.BLOCK_NOT_FOUND));
        if (!block.getPrice().equals(previewPrice)) {
            pointService.refund(userId, previewPrice, TransactionType.BLOCK_UNBLOCK,
                    "활동 차단 조기 해제 실패 - 가격 변경으로 환불");
            throw new CustomException(ErrorCode.BLOCK_PRICE_CHANGED);
        }
        block.lift();
    }

    @Transactional
    public void appeal(Long userId, BlockAppealRequest request) {
        User user = userService.findById(userId);
        ActivityBlock block = activityBlockRepository.findByTargetAndStatus(user, BlockStatus.ACTIVE)
                .filter(ActivityBlock::isCurrentlyActive)
                .orElseThrow(() -> new CustomException(ErrorCode.BLOCK_NOT_FOUND));

        if (blockAppealRepository.existsByBlockAndHandledFalse(block)) {
            throw new CustomException(ErrorCode.APPEAL_ALREADY_PENDING);
        }

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

    // unblock=true면 이의제기가 정당하다고 판단해 그 자리에서 차단을 풀어준다. 포인트는 환불하지
    // 않는다 - 차단권 구매 자체를 취소하는 게 아니라 관리자 재량으로 조기 해제해주는 구제 조치이므로
    // unblockSelf()의 포인트 차감과는 별개다.
    @Transactional
    public void markAppealHandled(Long appealId, boolean unblock) {
        BlockAppeal appeal = blockAppealRepository.findById(appealId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        appeal.markHandled();
        if (unblock) {
            ActivityBlock block = activityBlockRepository.findActiveByTargetForUpdate(appeal.getSender())
                    .orElseThrow(() -> new CustomException(ErrorCode.BLOCK_NOT_FOUND));
            block.lift();
        }
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
