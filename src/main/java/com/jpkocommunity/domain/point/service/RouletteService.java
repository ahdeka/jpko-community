package com.jpkocommunity.domain.point.service;

import com.jpkocommunity.domain.notification.entity.NotificationType;
import com.jpkocommunity.domain.notification.event.NotificationEvent;
import com.jpkocommunity.domain.point.dto.response.RouletteOutcome;
import com.jpkocommunity.domain.point.dto.response.TransactionResponse;
import com.jpkocommunity.domain.point.entity.TransactionType;
import com.jpkocommunity.domain.point.repository.TransactionRepository;
import com.jpkocommunity.domain.user.service.UserService;
import com.jpkocommunity.global.config.RouletteProperties;
import com.jpkocommunity.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

// 로그인 시 하루 1회, 무작위로 뽑힌 다른 유저의 포인트를 강탈해오는 룰렛
@Slf4j
@Service
@RequiredArgsConstructor
public class RouletteService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final UserService userService;
    private final RouletteProperties rouletteProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public RouletteOutcome spinIfEligible(Long userId) {
        // 락 없는 사전 체크 -> 하루 중 두 번째 이후 로그인은 대부분 여기서 끝남
        if (alreadyPlayedToday(userId)) {
            return RouletteOutcome.none();
        }

        // 피해자 후보 없음(활성 유저가 본인 1명) -> 오늘의 기회를 소모하지 않고 스킵
        Optional<Long> victimId = userService.findRandomActiveUserIdExcluding(userId);
        if (victimId.isEmpty()) {
            return RouletteOutcome.none();
        }

        long rolledAmount = ThreadLocalRandom.current()
                .nextLong(rouletteProperties.minAmount(), rouletteProperties.maxAmount() + 1);

        // 1단계: 피해자 -> 시스템 (몰수). 포인트는 항상 시스템 계정을 거쳐야 하므로 유저-유저 직접 이체는 하지 않는다.
        TransactionResponse collected;
        try {
            collected = transactionService.collectForRoulette(victimId.get(), rolledAmount, "일일 포인트 룰렛 몰수");
        } catch (CustomException e) {
            // 피해자가 몰수 도중 정지되는 등 드문 레이스 -> 아직 아무 것도 옮기지 않았으므로 그냥 스킵
            log.warn("[포인트 룰렛] 몰수 실패, 스킵 처리. winnerId={}, victimId={}", userId, victimId.get(), e);
            return RouletteOutcome.none();
        }
        long actualAmount = collected.amount();

        // 2단계: 시스템 -> 당첨자 (지급). 당첨자 계좌 락 이후 "오늘 이미 지급받았는지"를 재검증한다.
        Optional<TransactionResponse> granted;
        try {
            granted = transactionService.grantForRouletteIfEligible(userId, actualAmount, "일일 포인트 룰렛 당첨");
        } catch (CustomException e) {
            // 당첨자가 지급 도중 정지되는 등 드문 레이스 -> 아래 환불 분기로 흘려보냄
            log.warn("[포인트 룰렛] 지급 실패, 환불 처리. winnerId={}, victimId={}", userId, victimId.get(), e);
            granted = Optional.empty();
        }

        if (granted.isEmpty()) {
            // 검증 실패(동시 로그인으로 이미 오늘 지급됨 등) -> 이미 몰수한 만큼 피해자에게 환불
            if (actualAmount > 0) {
                transactionService.grant(victimId.get(), actualAmount, "일일 포인트 룰렛 환불 - 중복 지급 감지");
            }
            return RouletteOutcome.none();
        }

        if (actualAmount > 0) {
            eventPublisher.publishEvent(new NotificationEvent(
                    victimId.get(), userId, NotificationType.POINT_STOLEN, null, null, false, actualAmount
            ));
        }

        return RouletteOutcome.spun(actualAmount);
    }

    private boolean alreadyPlayedToday(Long userId) {
        return transactionRepository.existsByRecipientIdAndTypeAndCreatedAtGreaterThanEqual(
                userId, TransactionType.ROULETTE, LocalDate.now(KST).atStartOfDay());
    }
}
