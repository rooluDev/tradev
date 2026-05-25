package com.tradev.domain.reservation.dto;

import com.tradev.domain.reservation.entity.Reservation;
import com.tradev.domain.reservation.entity.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationResponse(
    Long id,
    SlotInfo slot,
    UserInfo buyer,
    UserInfo seller,
    ReservationStatus status,
    String message,
    String cancelReason,
    LocalDateTime createdAt
) {
    public record SlotInfo(
        Long id,
        LocalDateTime startedAt,
        LocalDateTime endedAt
    ) {}

    public record UserInfo(
        Long id,
        String nickname,
        String profileImageUrl
    ) {}

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
            reservation.getId(),
            new SlotInfo(
                reservation.getSlot().getId(),
                reservation.getSlot().getStartedAt(),
                reservation.getSlot().getEndedAt()
            ),
            new UserInfo(
                reservation.getBuyer().getId(),
                reservation.getBuyer().getNickname(),
                reservation.getBuyer().getProfileImageUrl()
            ),
            new UserInfo(
                reservation.getSeller().getId(),
                reservation.getSeller().getNickname(),
                reservation.getSeller().getProfileImageUrl()
            ),
            reservation.getStatus(),
            reservation.getMessage(),
            reservation.getCancelReason(),
            reservation.getCreatedAt()
        );
    }
}
