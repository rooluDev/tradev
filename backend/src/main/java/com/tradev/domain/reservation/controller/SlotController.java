package com.tradev.domain.reservation.controller;

import com.tradev.common.response.ApiResponse;
import com.tradev.domain.reservation.dto.SlotRequest;
import com.tradev.domain.reservation.dto.SlotResponse;
import com.tradev.domain.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
public class SlotController {

    private final ReservationService reservationService;

    /** 슬롯 일괄 생성 */
    @PostMapping
    public ResponseEntity<ApiResponse<List<SlotResponse>>> createSlots(
        @AuthenticationPrincipal Long userId,
        @Valid @RequestBody SlotRequest.BatchCreate request
    ) {
        List<SlotResponse> result = reservationService.createSlots(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    /** 판매자 슬롯 월별 조회 */
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<ApiResponse<List<SlotResponse>>> getSlotsByMonth(
        @PathVariable Long sellerId,
        @RequestParam int year,
        @RequestParam int month
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(reservationService.getSlotsByMonth(sellerId, year, month))
        );
    }

    /** 판매자 예약 가능 슬롯 조회 */
    @GetMapping("/seller/{sellerId}/available")
    public ResponseEntity<ApiResponse<List<SlotResponse>>> getAvailableSlots(
        @PathVariable Long sellerId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(reservationService.getAvailableSlots(sellerId))
        );
    }

    /** 슬롯 삭제 */
    @DeleteMapping("/{slotId}")
    public ResponseEntity<Void> deleteSlot(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long slotId
    ) {
        reservationService.deleteSlot(userId, slotId);
        return ResponseEntity.noContent().build();
    }
}
