package com.tradev.domain.scheduler;

import com.tradev.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {

    private final NotificationRepository notificationRepository;

    /**
     * 매일 새벽 3시 — 30일 지난 읽은 알림 삭제
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime before = LocalDateTime.now().minusDays(30);
        notificationRepository.deleteOldReadNotifications(before);
        log.info("[Scheduler] 오래된 읽은 알림 삭제 완료 (before={})", before);
    }
}
