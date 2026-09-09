package com.jpkocommunity.domain.level.service;

import com.jpkocommunity.domain.level.entity.ExpAction;
import com.jpkocommunity.domain.level.entity.UserLevel;
import com.jpkocommunity.domain.level.repository.UserLevelRepository;
import com.jpkocommunity.domain.notification.entity.NotificationType;
import com.jpkocommunity.domain.notification.event.NotificationEvent;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.domain.level.repository.UserExp;
import com.jpkocommunity.global.config.properties.LevelProperties;
import com.jpkocommunity.global.util.KstDateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LevelService {

    private final UserLevelRepository userLevelRepository;
    private final LevelProperties levelProperties;
    private final KstDateUtils kstDateUtils;
    private final ApplicationEventPublisher eventPublisher;

    // 여러 유저의 현재 레벨을 한 번에 조회
    @Transactional(readOnly = true)
    public Map<Long, Integer> levelsOf(Collection<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();

        return userLevelRepository.findExpByUserIdIn(List.copyOf(userIds)).stream()
                .collect(Collectors.toMap(
                        UserExp::getUserId,
                        row -> LevelPolicy.levelOfExp(row.getExp())
                ));
    }

    // 유저의 현재 레벨 조회 단건용
    @Transactional(readOnly = true)
    public int levelOf(Long userId) {
        return userLevelRepository.findByUserId(userId)
                .map(ul -> LevelPolicy.levelOfExp(ul.getExp()))
                .orElse(LevelPolicy.MIN_LEVEL);
    }

    public void createUserLevel(User user) {
        userLevelRepository.save(UserLevel.builder()
                .user(user)
                .build());
    }

    public long gainExp(Long userId, ExpAction action) {
        long amount = levelProperties.expOf(action);
        return applyExp(userId, action.isDailyLimited()
                ? ul -> ul.gainExp(amount, levelProperties.dailyLimit(), kstDateUtils.today())
                : ul -> ul.gainExpIgnoringDailyLimit(amount));
    }

    // ========== private 메서드 ==========

    // 락 -> 경험치 지급 -> 레벨업 알림(가능하다면)
    private long applyExp(Long userId, ToLongFunction<UserLevel> grant) {
        UserLevel userLevel = userLevelRepository.findByUserIdForUpdate(userId).orElse(null);

        if (userLevel == null) {
            log.warn("[유저레벨] 유저 레벨 정보를 찾을 수 없음. userId: {}", userId);
            return 0;
        }

        long expBefore = userLevel.getExp();
        long granted = grant.applyAsLong(userLevel);

        // 일일 상한에 걸려 exp가 0으로 들어온 경우
        if (granted == 0) return 0;

        int previousLevel = LevelPolicy.levelOfExp(expBefore);
        int currentLevel = LevelPolicy.levelOfExp(userLevel.getExp());

        // 레벨업 알림 이벤트 발행
        if (currentLevel > previousLevel) {
            eventPublisher.publishEvent(new NotificationEvent(
                    userId, null, NotificationType.LEVEL_UP, null, null, false, (long) currentLevel
            ));
        }
        return granted;
    }
}
