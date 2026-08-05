package com.jpkocommunity.domain.notification.service;


import com.jpkocommunity.domain.notification.dto.response.NotificationResponse;
import com.jpkocommunity.domain.notification.entity.Notification;
import com.jpkocommunity.domain.notification.repository.NotificationRepository;
import com.jpkocommunity.global.exception.CustomException;
import com.jpkocommunity.global.exception.ErrorCode;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        Notification notification = getNotificationById(notificationId);

        validateOwner(userId, notification);

        notification.markAsRead();
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = getNotificationById(notificationId);

        validateOwner(userId, notification);

        notificationRepository.delete(notification);
    }

    @Transactional
    public void deleteAllNotifications(Long userId) {
        notificationRepository.deleteAllByReceiverId(userId);
    }

    public Page<NotificationResponse> getAllNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findAllByReceiverId(userId, pageable)
                .map(NotificationResponse::from);
    }

    // ========== private 메서드 ==========

    private static void validateOwner(Long userId, Notification notification) {
        if (!notification.getReceiver().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    @Nonnull
    private Notification getNotificationById(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

}
