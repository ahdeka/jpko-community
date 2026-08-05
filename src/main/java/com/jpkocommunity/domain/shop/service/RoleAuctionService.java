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
        // ROOT가 낙찰되면 grantRole()이 targetRole(ADMIN)로 강등시켜버린다 - 실수로라도 입찰 자체를
        // 막는다.
        if (bidder.getRole() == UserRole.ROOT) {
            throw new CustomException(ErrorCode.CANNOT_BID_AS_ROOT);
        }
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
        // 상태만 CANCELLED로 바꾸면 (round_id, bidder_user_id) unique 제약 때문에 같은 회차 재입찰이
        // 항상 DataIntegrityViolationException으로 실패한다. CANCELLED 상태는 어디서도 조회되지 않으므로
        // row 자체를 삭제해 재입찰이 가능하게 한다.
        bidRepository.delete(bid);
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
    // settleRoundSafely와 동일하게 락을 걸고 조회한다 - 락 없이 조회하면 이 요청이 스케줄러의 정산
    // 틱과 시간상 겹칠 때 같은 회차가 두 번 정산되어(낙찰자 기간 이중 연장, 유찰자 이중 환불,
    // 알림 중복 발송) 버릴 수 있다.
    @Transactional
    public void updateRoundEndAt(Long roundId, LocalDateTime newEndAt) {
        RoleAuctionRound round = roundRepository.findByIdAndStatusForUpdate(roundId)
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

    // 정산 대상 회차 id만 락 없이 조회한다. 실제 정산은 회차마다 독립된 트랜잭션(settleRoundSafely)에서
    // 이뤄져야 한 회차(예: 낙찰 실패자가 정지/탈퇴 상태라 환불이 거부되는 경우)의 실패가 같은 tick에
    // 묶인 다른 회차의 정산까지 막지 않는다. 스케줄러(RoleAuctionScheduler)가 이 목록을 받아 회차마다
    // settleRoundSafely를 호출한다 - 같은 빈 안에서 this.settleRoundSafely(...)로 호출하면 Spring AOP
    // 프록시를 거치지 않아 @Transactional이 무시되므로, 반드시 스케줄러(다른 빈)가 호출해야 한다.
    public List<Long> findExpiredRoundIds() {
        return roundRepository.findAllOpenAndEndedIds(LocalDateTime.now());
    }

    // 회차 하나만 락을 걸고 정산하는 독립 트랜잭션. 여기서 예외를 잡아 삼키면 안 된다 - refund()가
    // 던진 예외는 이미 이 메서드의 물리 트랜잭션을 rollback-only로 마킹한 뒤라, 여기서 catch해서
    // 정상 반환해도 커밋 시점에 UnexpectedRollbackException이 새로 터져서 호출자(스케줄러)의 반복문을
    // 중단시킨다. 그러니 예외는 그대로 던져서 이 메서드가 곧바로(정상적으로) 롤백되게 하고, catch는
    // 이 메서드의 트랜잭션 경계 바깥인 RoleAuctionScheduler에서 한다.
    // 실패한 회차는 status가 여전히 OPEN이라 다음 tick에 findExpiredRoundIds()에 다시 걸려 재시도된다.
    @Transactional
    public void settleRoundSafely(Long roundId) {
        roundRepository.findByIdAndStatusForUpdate(roundId)
                .filter(RoleAuctionRound::isOpenAndEnded)
                .ifPresent(this::settleRound);
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
    // cadenceDays(회차 사이 최소 간격)가 설정돼 있으면, 가장 최근에 정산된 회차의 종료 시각 기준으로
    // 그만큼 지나야 다음 회차를 연다 - 그 전이면 아직 개설하지 않고 다음 tick에 다시 판단한다.
    @Transactional
    public void openRoundIfNeeded(ShopItem item) {
        boolean hasOpenRound = roundRepository.findByShopItemAndStatus(item, AuctionRoundStatus.OPEN).isPresent();
        if (hasOpenRound) {
            return;
        }

        boolean cadenceElapsed = roundRepository.findTopByShopItemAndStatusOrderByEndAtDesc(item, AuctionRoundStatus.SETTLED)
                .map(lastRound -> !LocalDateTime.now().isBefore(lastRound.getEndAt().plusDays(item.getCadenceDays())))
                .orElse(true); // 이전에 정산된 회차가 없으면(첫 회차) 즉시 개설

        if (cadenceElapsed) {
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
        Optional<RolePurchase> existing = purchaseRepository.findByUserAndStatus(user, RolePurchaseStatus.ACTIVE);
        if (existing.isPresent() && existing.get().getTargetRole() == targetRole) {
            existing.get().extendByDays(ROLE_GRANT_DAYS);
        } else {
            // targetRole이 다른 경매 상품에 동시에 낙찰되는 등 기존 건과 대상 role이 다르면,
            // 기간만 늘려선 안 되고(previousRole/targetRole 스냅샷이 어긋남) 기존 건은 만료
            // 처리하고 새로 시작한다.
            existing.ifPresent(RolePurchase::expire);
            purchaseRepository.save(RolePurchase.create(
                    user, targetRole, user.getRole(), LocalDateTime.now().plusDays(ROLE_GRANT_DAYS)));
        }
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
