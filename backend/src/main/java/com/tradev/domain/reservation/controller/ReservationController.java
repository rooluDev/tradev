package com.tradev.domain.reservation.controller;

import com.tradev.common.dto.CursorPageResponse;
import com.tradev.common.response.ApiResponse;
import com.tradev.domain.reservation.dto.ReservationRequest;
import com.tradev.domain.reservation.dto.ReservationResponse;
import com.tradev.domain.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /** 예약 요청 */
    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(
        @AuthenticationPrincipal Long userId,
        @Valid @RequestBody ReservationRequest.Create request
    ) {
        ReservationResponse result = reservationService.createReservation(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    /** 내 예약 목록 */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CursorPageResponse<ReservationResponse>>> getMyReservations(
        @AuthenticationPrincipal Long userId,
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(reservationService.getMyReservations(userId, cursor, size))
        );
    }

    /** 예약 상세 조회 */
    @GetMapping("/{reservationId}")
    public ResponseEntity<ApiResponse<ReservationResponse>> getReservation(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long reservationId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(reservationService.getReservation(userId, reservationId))
        );
    }

    /** 예약 수락 (판매자) */
    @PatchMapping("/{reservationId}/accept")
    public ResponseEntity<ApiResponse<ReservationResponse>> acceptReservation(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long reservationId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(reservationService.acceptReservation(userId, reservationId))
        );
    }

    /** 예약 완료 처리 */
    @PatchMapping("/{reservationId}/complete")
    public ResponseEntity<ApiResponse<ReservationResponse>> completeReservation(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long reservationId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(reservationService.completeReservation(userId, reservationId))
        );
    }

    /** 예약 취소 */
    @PatchMapping("/{reservationId}/cancel")
    public ResponseEntity<ApiResponse<ReservationResponse>> cancelReservation(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long reservationId,
        @RequestBody(required = false) ReservationRequest.Cancel request
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(reservationService.cancelReservation(userId, reservationId, request))
        );
    }
}
