package com.tradev.domain.notification.controller;

import com.tradev.common.dto.CursorPageResponse;
import com.tradev.common.response.ApiResponse;
import com.tradev.domain.notification.dto.NotificationResponse;
import com.tradev.domain.notification.service.NotificationService;
import com.tradev.domain.notification.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;

    /** SSE 연결 */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
        @AuthenticationPrincipal Long userId
    ) {
        return sseEmitterService.connect(userId);
    }

    /** 알림 목록 */
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<NotificationResponse>>> getNotifications(
        @AuthenticationPrincipal Long userId,
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(notificationService.getNotifications(userId, cursor, size))
        );
    }

    /** 안 읽은 알림 수 */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
        @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(Map.of("count", notificationService.getUnreadCount(userId)))
        );
    }

    /** 특정 알림 읽음 처리 */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long notificationId
    ) {
        notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.noContent().build();
    }

    /** 전체 읽음 처리 */
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
        @AuthenticationPrincipal Long userId
    ) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }
}
