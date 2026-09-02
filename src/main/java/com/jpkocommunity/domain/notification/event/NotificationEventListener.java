package com.jpkocommunity.domain.notification.event;


import com.jpkocommunity.domain.notification.sse.NotificationPushDto;
import com.jpkocommunity.domain.notification.sse.SseEmitterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationWriter notificationWriter;
    private final SseEmitterRepository sseEmitterRepository;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(NotificationEvent event) {

        try {
            notificationWriter.write(event).ifPresent(saved -> {
                sseEmitterRepository.sendTo(
                        event.receiverId(), "notification", NotificationPushDto.of(saved, event)
                );
                log.info("[알림 생성] receiverId={}, type: {}, postId={}",
                        event.receiverId(), event.type(), event.postId());
            });

        } catch (Exception e) {
            // 알림 DB 저장 실패만 여기서 흡수
            log.error("[알림 처리 실패] event={}", event, e);
        }

    }

}
