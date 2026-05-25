package com.tradev.domain.notification.service;

import com.tradev.common.dto.CursorPageResponse;
import com.tradev.common.exception.ErrorCode;
import com.tradev.common.exception.TradevException;
import com.tradev.domain.notification.dto.NotificationResponse;
import com.tradev.domain.notification.entity.Notification;
import com.tradev.domain.notification.entity.NotificationType;
import com.tradev.domain.notification.repository.NotificationRepository;
import com.tradev.domain.user.entity.User;
import com.tradev.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SseEmitterService sseEmitterService;

    /**
     * 알림 저장 + SSE 실시간 전송
     */
    @Transactional
    public void createAndSend(Long userId, NotificationType type, String message, String link) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new TradevException(ErrorCode.USER_NOT_FOUND));

        Notification notification = Notification.builder()
            .user(user)
            .type(type)
            .message(message)
            .link(link)
            .build();
        notificationRepository.save(notification);

        // SSE 전송 (연결 중인 경우만)
        sseEmitterService.send(userId, Map.of(
            "id", notification.getId(),
            "type", type.name(),
            "message", message,
            "link", link != null ? link : ""
        ));
    }

    /**
     * 알림 목록 (커서 페이지네이션)
     */
    public CursorPageResponse<NotificationResponse> getNotifications(Long userId, String cursor, int size) {
        LocalDateTime cursorCreatedAt = null;
        Long cursorId = null;
        if (cursor != null) {
            String[] parts = cursor.split("\\|");
            if (parts.length == 2) {
                cursorCreatedAt = LocalDateTime.parse(parts[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                cursorId = Long.parseLong(parts[1]);
            }
        }

        List<Notification> notifications = notificationRepository.findByUserWithCursor(
            userId, cursorCreatedAt, cursorId, size + 1
        );
        List<NotificationResponse> responses = notifications.stream()
            .map(NotificationResponse::from)
            .collect(Collectors.toList());

        return CursorPageResponse.of(responses, size,
            r -> r.createdAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "|" + r.id());
    }

    /**
     * 안 읽은 알림 수
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * 특정 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new TradevException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.getUser().getId().equals(userId)) {
            throw new TradevException(ErrorCode.NOTIFICATION_NOT_OWNER);
        }
        notification.markAsRead();
    }

    /**
     * 전체 알림 읽음 처리
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}
