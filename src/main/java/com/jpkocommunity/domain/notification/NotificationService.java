package com.jpkocommunity.domain.notification;


import com.jpkocommunity.domain.notification.dto.response.NotificationResponse;
import com.jpkocommunity.domain.notification.entity.Notification;
import com.jpkocommunity.domain.notification.repository.NotificationRepository;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<NotificationResponse> getUnreadNotifications(Long receiverId) {
        return notificationRepository.findUnreadWithDetailsByReceiverId(receiverId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 다른 사람 알림을 읽음 처리하지 못하도록 소유자 검증
        if (!notification.getReceiver().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        notification.markAsRead();
    }
}
