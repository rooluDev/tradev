package com.tradev.domain.reservation.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class SlotRequest {

    public record Create(
        @NotNull(message = "시작 시간은 필수입니다")
        @Future(message = "시작 시간은 현재 이후여야 합니다")
        LocalDateTime startedAt,

        @NotNull(message = "종료 시간은 필수입니다")
        LocalDateTime endedAt
    ) {}

    public record BatchCreate(
        @NotNull(message = "슬롯 목록은 필수입니다")
        List<Create> slots
    ) {}
}
