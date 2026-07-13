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

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;


@Component
@RequiredArgsConstructor
public class NotificationWriter {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    // 원 트랜잭션과 별개의 트랜잭션에서 알림을 저장하도록 설정
    @Transactional(propagation = REQUIRES_NEW)
    public Notification write(NotificationEvent event) {
        Notification notification = Notification.builder()
                .receiver(userRepository.getReferenceById(event.receiverId()))
                .sender(userRepository.getReferenceById(event.senderId()))
                .type(event.type())
                .post(postRepository.getReferenceById(event.postId()))
                .comment(event.commentId() != null ? commentRepository.getReferenceById(event.commentId()) : null)
                .build();

        return notificationRepository.save(notification);
    }
}