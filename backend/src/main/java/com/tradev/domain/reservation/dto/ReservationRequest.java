package com.tradev.domain.reservation.dto;

import jakarta.validation.constraints.NotNull;

public class ReservationRequest {

    public record Create(
        @NotNull(message = "슬롯 ID는 필수입니다")
        Long slotId,

        String message
    ) {}

    public record Cancel(
        String reason
    ) {}
}
