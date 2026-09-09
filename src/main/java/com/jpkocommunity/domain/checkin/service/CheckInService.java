package com.jpkocommunity.domain.checkin.service;

import com.jpkocommunity.domain.checkin.dto.response.CheckInCalendarResponse;
import com.jpkocommunity.domain.checkin.dto.response.CheckInOutcome;
import com.jpkocommunity.domain.checkin.entity.CheckInLog;
import com.jpkocommunity.domain.checkin.repository.CheckInLogRepository;
import com.jpkocommunity.domain.level.entity.ExpAction;
import com.jpkocommunity.domain.level.service.LevelService;
import com.jpkocommunity.domain.point.dto.LockedAccount;
import com.jpkocommunity.domain.point.entity.TransactionType;
import com.jpkocommunity.domain.point.service.PointService;
import com.jpkocommunity.global.config.properties.CheckInProperties;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import com.jpkocommunity.global.util.KstDateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInService {

    private final CheckInLogRepository checkInLogRepository;
    private final PointService pointService;
    private final LevelService levelService;
    private final CheckInProperties checkInProperties;
    private final KstDateUtils kstDateUtils;

    /**
     * 출석체크 메서드
     */
    @Transactional
    public CheckInOutcome checkIn(Long userId) {
        LocalDate today = kstDateUtils.today();

        // 락 들어가기 전 사전검사 - 성능을 위해(이미 출석한 유저의 재요청을 쳐내 락으로 보내지 않도록)
        validateNotCheckedIn(userId, today);

        // 계정이 잠기는 시점 - 정지 계정이라면 여기서 예외처리가 던져짐
        Map<Long, LockedAccount> locked = pointService.lockAccounts(null, userId);

        // 락 들어간 후 재검사 - 동시성 문제를 해결하기 위해
        validateNotCheckedIn(userId, today);

        LockedAccount system = locked.get(null);
        LockedAccount user = locked.get(userId);

        // 이 달의 누적 출석 횟수 - 저장하지 않고 COUNT로 셈 (아직 저장 전이기 때문에, 어제까지의 개수임)
        int monthlyCount = checkInLogRepository.countByUserIdAndCheckInDateBetween(
                userId, today.withDayOfMonth(1), today) + 1;
        long bonus = checkInProperties.bonusOf(monthlyCount);
        long total = checkInProperties.amount() + bonus;

        // 포인트 이동 + transactions 기록
        pointService.move(system, user, total, TransactionType.CHECKIN, messageOf(monthlyCount, bonus));
        checkInLogRepository.save(CheckInLog.builder()
                .user(user.user())
                .checkInDate(today)
                .build());

        long grantedExp = levelService.gainExp(userId, ExpAction.CHECK_IN);
        return new CheckInOutcome(total, bonus, monthlyCount, grantedExp);
    }

    /**
     * 출석 페이지용 월별 조회 메서드
     */
    @Transactional(readOnly = true)
    public CheckInCalendarResponse getMonthlyCheckIns(Long userId, Integer year, Integer month) {
        LocalDate today = kstDateUtils.today();

        YearMonth target = (year != null && month != null)
                ? YearMonth.of(year, month)
                : YearMonth.from(today);

        LocalDate from = target.atDay(1);
        LocalDate to = target.atEndOfMonth();

        List<Integer> checkedInDays = checkInLogRepository
                .findByUserIdAndCheckInDateBetweenOrderByCheckInDate(userId, from, to).stream()
                .map(log -> log.getCheckInDate().getDayOfMonth())
                .toList();

        int monthlyCount = checkedInDays.size();

        boolean checkedInToday = checkInLogRepository.existsByUserIdAndCheckInDate(userId, today);

        List<CheckInCalendarResponse.Milestone> milestones =
                checkInProperties.monthlyBonuses().stream()
                        .map(m -> new CheckInCalendarResponse.Milestone(
                                m.count(), m.bonus(), monthlyCount >= m.count()))
                        .toList();

        return new CheckInCalendarResponse(
                target.getYear(),
                target.getMonthValue(),
                target.lengthOfMonth(),
                checkedInDays,
                monthlyCount,
                checkedInToday,
                milestones);
    }

    // ========== private 메서드 ==========

    private void validateNotCheckedIn(Long userId, LocalDate today) {
        if (checkInLogRepository.existsByUserIdAndCheckInDate(userId, today)) {
            throw new CustomException(ErrorCode.ALREADY_CHECKED_IN);
        }
    }

    private String messageOf(int monthlyCount, long bonus) {
        return bonus == 0
                ? "일일 출첵"
                : "일일 출첵 (이달 %d일 달성 보너스 +%dP)".formatted(monthlyCount, bonus);
    }
}