package com.jpkocommunity.domain.shop.service;

import com.jpkocommunity.domain.notification.entity.NotificationType;
import com.jpkocommunity.domain.notification.event.NotificationEvent;
import com.jpkocommunity.domain.point.entity.TransactionType;
import com.jpkocommunity.domain.point.service.PointService;
import com.jpkocommunity.domain.shop.dto.request.PlaceBidRequest;
import com.jpkocommunity.domain.shop.dto.response.RoleAuctionRoundResponse;
import com.jpkocommunity.domain.shop.dto.response.RolePurchaseAdminResponse;
import com.jpkocommunity.domain.shop.dto.response.RolePurchaseStatusResponse;
import com.jpkocommunity.domain.shop.entity.AuctionRoundStatus;
import com.jpkocommunity.domain.shop.entity.BidStatus;
import com.jpkocommunity.domain.shop.entity.RoleAuctionRound;
import com.jpkocommunity.domain.shop.entity.RoleBid;
import com.jpkocommunity.domain.shop.entity.RolePurchase;
import com.jpkocommunity.domain.shop.entity.RolePurchaseStatus;
import com.jpkocommunity.domain.shop.entity.ShopItem;
import com.jpkocommunity.domain.shop.entity.ShopItemType;
import com.jpkocommunity.domain.shop.repository.RoleAuctionRoundRepository;
import com.jpkocommunity.domain.shop.repository.RoleBidRepository;
import com.jpkocommunity.domain.shop.repository.RolePurchaseRepository;
import com.jpkocommunity.domain.shop.repository.ShopItemRepository;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.user.entity.UserRole;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 포인트 기반 ADMIN 권한 경매(#7): 입찰/정산/회차개설/만료를 한 클래스에서 처리.
// 라이프사이클이 강하게 결합돼 있어(입찰 -> 정산 -> 권한부여 -> 만료복구) 별도 서비스로 쪼개지 않는다.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleAuctionService {

    // 낙찰 시 부여 기간. 요구사항상 항상 1주 단위이며 재낙찰 시 기존 만료일에서 이만큼 연장된다.
    private static final int ROLE_GRANT_DAYS = 7;

    private final RoleAuctionRoundRepository roundRepository;
    private final RoleBidRepository bidRepository;
    private final RolePurchaseRepository purchaseRepository;
    private final ShopItemRepository shopItemRepository;
    private final UserService userService;
    private final PointService pointService;
    private final ApplicationEventPublisher eventPublisher;

    public List<RoleAuctionRoundResponse> getCurrentRounds() {
        return roundRepository.findByStatus(AuctionRoundStatus.OPEN).stream()
                .map(round -> RoleAuctionRoundResponse.of(round, activeBidAmountsDesc(round)))
                .toList();
    }

    @Transactional
    public void placeBid(Long userId, Long roundId, PlaceBidRequest request) {
        RoleAuctionRound round = findOpenRound(roundId);
        User bidder = userService.findById(userId);
        long amount = request.amount();

        Optional<RoleBid> existing = bidRepository.findByRoundAndBidder(round, bidder)
                .filter(bid -> bid.getStatus() == BidStatus.ACTIVE);

        if (existing.isPresent()) {
            RoleBid bid = existing.get();
            if (amount <= bid.getAmount()) {
                throw new CustomException(ErrorCode.BID_TOO_LOW);
            }
            long delta = amount - bid.getAmount();
            pointService.charge(userId, delta, TransactionType.BID_HOLD, "권한 경매 입찰 인상 - 회차 " + roundId);
            bid.raiseTo(amount);
        } else {
            if (amount < round.getMinPrice()) {
                throw new CustomException(ErrorCode.BID_TOO_LOW);
            }
            pointService.charge(userId, amount, TransactionType.BID_HOLD, "권한 경매 입찰 - 회차 " + roundId);
            bidRepository.save(RoleBid.place(round, bidder, amount));
        }
    }

    @Transactional
    public void cancelBid(Long userId, Long roundId) {
        RoleAuctionRound round = findOpenRound(roundId);
        User bidder = userService.findById(userId);
        RoleBid bid = bidRepository.findByRoundAndBidder(round, bidder)
                .filter(b -> b.getStatus() == BidStatus.ACTIVE)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        pointService.refund(userId, bid.getAmount(), TransactionType.BID_REFUND, "권한 경매 입찰 취소 환불");
        bid.cancel();
    }

    public RolePurchaseStatusResponse getMyRoleStatus(Long userId) {
        User user = userService.findById(userId);
        return purchaseRepository.findByUserAndStatus(user, RolePurchaseStatus.ACTIVE)
                .map(RolePurchaseStatusResponse::from)
                .orElseGet(RolePurchaseStatusResponse::none);
    }

    // ========== 관리자 기능 ==========

    public Page<RolePurchaseAdminResponse> getActiveRolePurchases(Pageable pageable) {
        return purchaseRepository.findByStatus(RolePurchaseStatus.ACTIVE, pageable).map(RolePurchaseAdminResponse::from);
    }

    // ROOT가 진행 중인 회차의 종료 시각을 직접 수정(연장)하거나 조기 종료한다.
    // 새 시각이 현재 이하면(조기 종료) 스케줄러의 다음 tick(최대 5분)을 기다리지 않고 이 요청 안에서 바로 정산한다.
    @Transactional
    public void updateRoundEndAt(Long roundId, LocalDateTime newEndAt) {
        RoleAuctionRound round = roundRepository.findByIdAndStatus(roundId, AuctionRoundStatus.OPEN)
                .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_ROUND_NOT_OPEN));

        round.updateEndAt(newEndAt);

        if (!newEndAt.isAfter(LocalDateTime.now())) {
            settleRound(round);
            log.info("ROOT가 경매 회차를 조기 종료 - round={}", roundId);
        } else {
            log.info("ROOT가 경매 회차 종료 시각 변경 - round={}, newEndAt={}", roundId, newEndAt);
        }
    }

    // ========== 스케줄러 호출 ==========

    @Transactional
    public void settleExpiredRounds() {
        List<RoleAuctionRound> ended = roundRepository.findAllOpenAndEndedForUpdate(LocalDateTime.now());
        for (RoleAuctionRound round : ended) {
            settleRound(round);
        }
    }

    @Transactional
    public void openRoundsForActiveItems() {
        List<ShopItem> items = shopItemRepository.findByTypeAndActiveTrue(ShopItemType.ROLE_AUCTION);
        for (ShopItem item : items) {
            openRoundIfNeeded(item);
        }
    }

    // 상품 등록/비활성→활성 전환 시 ShopItemService가 곧바로 호출한다 — 스케줄러의 다음 tick(최대 5분)을
    // 기다리지 않고 즉시 첫 회차가 열리게 하기 위함. 이미 OPEN인 회차가 있으면 아무 것도 하지 않는다.
    @Transactional
    public void openRoundIfNeeded(ShopItem item) {
        boolean hasOpenRound = roundRepository.findByShopItemAndStatus(item, AuctionRoundStatus.OPEN).isPresent();
        if (!hasOpenRound) {
            roundRepository.save(RoleAuctionRound.open(item));
            log.info("권한 경매 회차 개설 - item={}, targetRole={}", item.getName(), item.getTargetRole());
        }
    }

    @Transactional
    public void expireRolePurchases() {
        List<RolePurchase> expired = purchaseRepository.findAllExpired(LocalDateTime.now());
        for (RolePurchase purchase : expired) {
            purchase.expire();
            purchase.getUser().updateRole(purchase.getPreviousRole());
            log.info("권한 경매로 획득한 role 자동 회수 - userId={}, {} -> {}",
                    purchase.getUser().getId(), purchase.getTargetRole(), purchase.getPreviousRole());
        }
    }

    // ========== private 메서드 ==========

    private void settleRound(RoleAuctionRound round) {
        List<RoleBid> activeBids = bidRepository.findByRoundAndStatusOrderByAmountDescCreatedAtAsc(round, BidStatus.ACTIVE);
        int winnerCount = Math.min(round.getQuantity(), activeBids.size());

        for (int i = 0; i < activeBids.size(); i++) {
            RoleBid bid = activeBids.get(i);
            if (i < winnerCount) {
                bid.win();
                grantRole(bid.getBidder(), round.getTargetRole());
                log.info("권한 경매 낙찰 - userId={}, round={}, amount={}, role={}",
                        bid.getBidder().getId(), round.getId(), bid.getAmount(), round.getTargetRole());
                eventPublisher.publishEvent(new NotificationEvent(
                        bid.getBidder().getId(), null, NotificationType.ROLE_AUCTION_WON, null, null, false, bid.getAmount()
                ));
            } else {
                bid.lose();
                pointService.refund(bid.getBidder().getId(), bid.getAmount(), TransactionType.BID_REFUND, "권한 경매 낙찰 실패 환불");
                eventPublisher.publishEvent(new NotificationEvent(
                        bid.getBidder().getId(), null, NotificationType.ROLE_AUCTION_LOST, null, null, false, bid.getAmount()
                ));
            }
        }

        round.settle();
    }

    private void grantRole(User user, UserRole targetRole) {
        purchaseRepository.findByUserAndStatus(user, RolePurchaseStatus.ACTIVE)
                .ifPresentOrElse(
                        existing -> existing.extendByDays(ROLE_GRANT_DAYS),
                        () -> purchaseRepository.save(RolePurchase.create(
                                user, targetRole, user.getRole(), LocalDateTime.now().plusDays(ROLE_GRANT_DAYS)))
                );
        user.updateRole(targetRole);
    }

    private RoleAuctionRound findOpenRound(Long roundId) {
        return roundRepository.findByIdAndStatus(roundId, AuctionRoundStatus.OPEN)
                .filter(round -> !round.isOpenAndEnded())
                .orElseThrow(() -> new CustomException(ErrorCode.AUCTION_ROUND_NOT_OPEN));
    }

    private List<Long> activeBidAmountsDesc(RoleAuctionRound round) {
        return bidRepository.findByRoundAndStatusOrderByAmountDescCreatedAtAsc(round, BidStatus.ACTIVE).stream()
                .map(RoleBid::getAmount)
                .toList();
    }
}
