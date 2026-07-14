package com.jpkocommunity.domain.notification.event;

import com.jpkocommunity.domain.comment.repository.CommentRepository;
import com.jpkocommunity.domain.notification.entity.Notification;
import com.jpkocommunity.domain.notification.entity.NotificationType;
import com.jpkocommunity.domain.notification.repository.NotificationRepository;
import com.jpkocommunity.domain.post.repository.PostRepository;
import com.jpkocommunity.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;


@Component
@RequiredArgsConstructor
public class NotificationWriter {

    // 좋아요 취소 -> 재클릭 알림을 억제하기 위한 시간 텀 설정
    private static final Duration LIKE_DEDUP_WINDOW = Duration.ofMinutes(1);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    // 원 트랜잭션과 별개의 트랜잭션에서 알림을 저장하도록 설정
    @Transactional(propagation = REQUIRES_NEW)
    public Optional<Notification> write(NotificationEvent event) {
        if (isSuppressed(event)) {
            return Optional.empty();
        }

        Notification notification = Notification.builder()
                .receiver(userRepository.getReferenceById(event.receiverId()))
                .sender(userRepository.getReferenceById(event.senderId()))
                .type(event.type())
                .post(postRepository.getReferenceById(event.postId()))
                .comment(event.commentId() != null ? commentRepository.getReferenceById(event.commentId()) : null)
                .build();

        return Optional.of(notificationRepository.save(notification));
    }

    private boolean isSuppressed(NotificationEvent event) {
        if (event.type() != NotificationType.LIKE) {
            return false;
        }

        return notificationRepository.findTopByReceiverIdAndSenderIdAndPostIdAndTypeOrderByCreatedAtDesc(
                        event.receiverId(), event.senderId(), event.postId(), event.type()
                )
                .map(last -> last.getCreatedAt().isAfter(LocalDateTime.now().minus(LIKE_DEDUP_WINDOW)))
                .orElse(false);
    }

}