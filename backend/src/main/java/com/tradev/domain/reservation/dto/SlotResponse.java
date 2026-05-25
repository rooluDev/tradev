package com.tradev.domain.reservation.dto;

import com.tradev.domain.reservation.entity.SlotStatus;
import com.tradev.domain.reservation.entity.TimeSlot;

import java.time.LocalDateTime;

public record SlotResponse(
    Long id,
    Long sellerId,
    String sellerNickname,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    SlotStatus status,
    LocalDateTime createdAt
) {
    public static SlotResponse from(TimeSlot slot) {
        return new SlotResponse(
            slot.getId(),
            slot.getSeller().getId(),
            slot.getSeller().getNickname(),
            slot.getStartedAt(),
            slot.getEndedAt(),
            slot.getStatus(),
            slot.getCreatedAt()
        );
    }
}
