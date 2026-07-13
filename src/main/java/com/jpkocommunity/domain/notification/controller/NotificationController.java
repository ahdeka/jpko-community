package com.jpkocommunity.domain.notification.controller;

import com.jpkocommunity.domain.notification.NotificationService;
import com.jpkocommunity.domain.notification.dto.response.NotificationResponse;
import com.jpkocommunity.domain.notification.sse.SseEmitterRepository;
import com.jpkocommunity.global.response.ApiResponse;
import com.jpkocommunity.global.security.auth.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        SseEmitter emitter = sseEmitterRepository.save(authUser.userId());

        // 연결 직후 더미 이벤트 전송
        try {
            // 초기 연결 시, 클라이언트에게 연결 성공 이벤트 전송
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getUnreadNotifications(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ApiResponse.ok(notificationService.getUnreadNotifications(authUser.userId()));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long notificationId
    ) {
        notificationService.markAsRead(authUser.userId(), notificationId);
        return ApiResponse.ok("알림이 읽음 처리되었습니다.");
    }
}
