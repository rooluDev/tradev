package com.tradev.domain.notification.repository;

import com.tradev.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
        SELECT n FROM Notification n
        WHERE n.user.id = :userId
          AND (:cursorCreatedAt IS NULL OR n.createdAt < :cursorCreatedAt
               OR (n.createdAt = :cursorCreatedAt AND n.id < :cursorId))
        ORDER BY n.createdAt DESC, n.id DESC
        LIMIT :pageSize
        """)
    List<Notification> findByUserWithCursor(
        @Param("userId") Long userId,
        @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
        @Param("cursorId") Long cursorId,
        @Param("pageSize") int pageSize
    );

    long countByUserIdAndIsReadFalse(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);

    /** 30일 이상 지난 읽은 알림 삭제 (스케줄러용) */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.createdAt < :before")
    void deleteOldReadNotifications(@Param("before") LocalDateTime before);
}
