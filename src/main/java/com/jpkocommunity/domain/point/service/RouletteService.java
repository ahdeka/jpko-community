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

        Optional<TransactionResponse> result;
        try {
            result = transactionService.spinRoulette(userId, victimId.get(), rolledAmount);
        } catch (CustomException e) {
            // 피해자가 스핀 도중 정지되는 등 드문 레이스 -> 로그인 자체는 계속 진행되도록 흡수
            log.warn("[포인트 룰렛] 스핀 실패, 스킵 처리. winnerId={}, victimId={}", userId, victimId.get(), e);
            return RouletteOutcome.none();
        }

        // 락 이후 재확인에서 "이미 오늘 돌림"으로 판정된 경쟁 상황
        if (result.isEmpty()) {
            return RouletteOutcome.none();
        }

        TransactionResponse response = result.get();
        if (response.amount() > 0) {
            eventPublisher.publishEvent(new NotificationEvent(
                    victimId.get(), userId, NotificationType.POINT_STOLEN, null, null, false, response.amount()
            ));
        }

        return RouletteOutcome.spun(response.amount());
    }

    private boolean alreadyPlayedToday(Long userId) {
        return transactionRepository.existsByRecipientIdAndTypeAndCreatedAtGreaterThanEqual(
                userId, TransactionType.ROULETTE, LocalDate.now(KST).atStartOfDay());
    }
}
