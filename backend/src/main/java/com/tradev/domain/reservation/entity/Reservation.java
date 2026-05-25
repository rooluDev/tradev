package com.tradev.domain.reservation.entity;

import com.tradev.common.entity.BaseTimeEntity;
import com.tradev.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reservations",
    indexes = {
        @Index(name = "idx_reservation_buyer", columnList = "buyer_id"),
        @Index(name = "idx_reservation_seller", columnList = "seller_id"),
        @Index(name = "idx_reservation_slot", columnList = "slot_id")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private TimeSlot slot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(length = 500)
    private String message;

    @Column(length = 500)
    private String cancelReason;

    @Builder
    public Reservation(TimeSlot slot, User buyer, User seller, String message) {
        this.slot = slot;
        this.buyer = buyer;
        this.seller = seller;
        this.message = message;
    }

    public void accept() {
        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel(String reason) {
        this.status = ReservationStatus.CANCELLED;
        this.cancelReason = reason;
    }

    public void complete() {
        this.status = ReservationStatus.COMPLETED;
    }

    public boolean isBuyer(Long userId) {
        return buyer.getId().equals(userId);
    }

    public boolean isSeller(Long userId) {
        return seller.getId().equals(userId);
    }

    public boolean isParticipant(Long userId) {
        return isBuyer(userId) || isSeller(userId);
    }
}
