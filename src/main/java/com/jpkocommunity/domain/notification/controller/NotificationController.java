package com.jpkocommunity.domain.notification.controller;

import com.jpkocommunity.domain.notification.service.NotificationService;
import com.jpkocommunity.domain.notification.dto.response.NotificationResponse;
import com.jpkocommunity.domain.notification.sse.SseEmitterRepository;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SseEmitterRepository sseEmitterRepository;
    private final NotificationService notificationService;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return sseEmitterRepository.save(authUser.userId());
    }

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getUnreadNotifications(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ApiResponse.ok(notificationService.getUnreadNotifications(authUser.userId()));
    }

    @GetMapping("/all")
    public ApiResponse<Page<NotificationResponse>> getAllNotifications(
            @AuthenticationPrincipal AuthUser authUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.ok(notificationService.getAllNotifications(authUser.userId(), pageable));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long notificationId
    ) {
        notificationService.markAsRead(authUser.userId(), notificationId);
        return ApiResponse.ok("알림이 읽음 처리되었습니다.");
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        notificationService.markAllAsRead(authUser.userId());
        return ApiResponse.ok("모든 알림을 읽음 처리했습니다.");
    }

    @DeleteMapping("/{notificationId}")
    public ApiResponse<Void> deleteNotification(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long notificationId
    ) {
        notificationService.deleteNotification(authUser.userId(), notificationId);
        return ApiResponse.ok("알림이 삭제되었습니다.");
    }

    @DeleteMapping("/all")
    public ApiResponse<Void> deleteAllNotification(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        notificationService.deleteAllNotifications(authUser.userId());
        return ApiResponse.ok("모든 알림이 삭제되었습니다.");
    }

}
