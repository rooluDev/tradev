package com.tradev.domain.notification.entity;

import com.tradev.common.entity.BaseTimeEntity;
import com.tradev.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications",
    indexes = {
        @Index(name = "idx_notification_user_read", columnList = "user_id, is_read"),
        @Index(name = "idx_notification_created", columnList = "user_id, created_at")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String message;

    /** 연관 리소스 URL (예: /trades/42, /reservations/7) */
    @Column(length = 200)
    private String link;

    @Column(nullable = false)
    private boolean isRead = false;

    @Builder
    public Notification(User user, NotificationType type, String message, String link) {
        this.user = user;
        this.type = type;
        this.message = message;
        this.link = link;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
