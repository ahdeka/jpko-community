package com.jpkocommunity.domain.notification.event;


import com.jpkocommunity.domain.comment.repository.CommentRepository;
import com.jpkocommunity.domain.notification.entity.Notification;
import com.jpkocommunity.domain.notification.repository.NotificationRepository;
import com.jpkocommunity.domain.notification.sse.NotificationPushDto;
import com.jpkocommunity.domain.notification.sse.SseEmitterRepository;
import com.jpkocommunity.domain.post.repository.PostRepository;
import com.jpkocommunity.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationWriter notificationWriter;
    private final SseEmitterRepository sseEmitterRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(NotificationEvent event) {

        try {
            Notification saved = notificationWriter.write(event);
            sseEmitterRepository.sendTo(
                    event.receiverId(), "notification", NotificationPushDto.of(saved, event)
            );

            log.info("[알림 생성] receiverId={}, type: {}, postId={}",
                    event.receiverId(), event.type(), event.postId());
        } catch (Exception e) {
            // DB 저장 실패 or SSE 전송 실패 모두 여기서 흡수
            log.error("[알림 처리 실패] event={}", event, e);
        }

    }

}
