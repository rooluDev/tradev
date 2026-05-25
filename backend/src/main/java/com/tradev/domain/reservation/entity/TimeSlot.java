package com.tradev.domain.reservation.entity;

import com.tradev.common.entity.BaseTimeEntity;
import com.tradev.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "time_slots",
    uniqueConstraints = @UniqueConstraint(columnNames = {"seller_id", "started_at"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimeSlot extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SlotStatus status = SlotStatus.AVAILABLE;

    @Version
    private Long version;

    @Builder
    public TimeSlot(User seller, LocalDateTime startedAt, LocalDateTime endedAt) {
        this.seller = seller;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    public void lock() { this.status = SlotStatus.LOCKED; }
    public void unlock() { this.status = SlotStatus.AVAILABLE; }
    public void reserve() { this.status = SlotStatus.RESERVED; }
    public boolean isAvailable() { return this.status == SlotStatus.AVAILABLE; }
}
