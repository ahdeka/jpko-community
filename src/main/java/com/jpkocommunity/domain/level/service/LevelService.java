package com.jpkocommunity.domain.level.service;

import com.jpkocommunity.domain.level.entity.ExpAction;
import com.jpkocommunity.domain.level.entity.UserLevel;
import com.jpkocommunity.domain.level.repository.UserLevelRepository;
import com.jpkocommunity.domain.notification.entity.NotificationType;
import com.jpkocommunity.domain.notification.event.NotificationEvent;
import com.jpkocommunity.domain.user.entity.User;
import com.jpkocommunity.global.config.properties.LevelProperties;
import com.jpkocommunity.global.util.KstDateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LevelService {

    private final UserLevelRepository userLevelRepository;
    private final LevelProperties levelProperties;
    private final KstDateUtils kstDateUtils;
    private final ApplicationEventPublisher eventPublisher;

    public void createUserLevel(User user) {
        userLevelRepository.save(UserLevel.builder()
                .user(user)
                .build());
    }

    public void gainExp(Long userId, ExpAction action) {
        UserLevel userLevel = userLevelRepository.findByUserIdForUpdate(userId).orElse(null);

        long expBefore = userLevel.getExp();

        long grantedExp = userLevel.gainExp(
                levelProperties.expOf(action),
                levelProperties.dailyLimit(),
                kstDateUtils.today()
        );

        // 일일 상한에 걸려 exp가 0으로 들어온 경우
        if (grantedExp == 0) return;

        int previousLevel = LevelPolicy.levelOfExp(expBefore);
        int currentLevel = LevelPolicy.levelOfExp(userLevel.getExp());

        // 레벨업 알림 이벤트 발행
        if (currentLevel > previousLevel) {
            eventPublisher.publishEvent(new NotificationEvent(
                    userId, null, NotificationType.LEVEL_UP, null, null, false, (long) currentLevel
            ));
        }
    }
}
