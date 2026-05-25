package com.tradev.domain.reservation.service;

import com.tradev.common.dto.CursorPageResponse;
import com.tradev.common.exception.ErrorCode;
import com.tradev.common.exception.TradevException;
import com.tradev.domain.reservation.dto.ReservationRequest;
import com.tradev.domain.reservation.dto.ReservationResponse;
import com.tradev.domain.reservation.dto.SlotRequest;
import com.tradev.domain.reservation.dto.SlotResponse;
import com.tradev.domain.reservation.entity.Reservation;
import com.tradev.domain.reservation.entity.ReservationStatus;
import com.tradev.domain.reservation.entity.SlotStatus;
import com.tradev.domain.reservation.entity.TimeSlot;
import com.tradev.domain.reservation.repository.ReservationRepository;
import com.tradev.domain.reservation.repository.TimeSlotRepository;
import com.tradev.domain.user.entity.User;
import com.tradev.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final TimeSlotRepository timeSlotRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final SlotLockService slotLockService;
    private final ApplicationEventPublisher eventPublisher;

    // ───────────────────────────── Slot ─────────────────────────────

    @Transactional
    public List<SlotResponse> createSlots(Long sellerId, SlotRequest.BatchCreate request) {
        User seller = getUser(sellerId);

        List<TimeSlot> slots = request.slots().stream()
            .map(req -> {
                validateSlotTime(req.startedAt(), req.endedAt());
                if (timeSlotRepository.existsBySellerIdAndStartedAt(sellerId, req.startedAt())) {
                    throw new TradevException(ErrorCode.SLOT_DUPLICATE);
                }
                return TimeSlot.builder()
                    .seller(seller)
                    .startedAt(req.startedAt())
                    .endedAt(req.endedAt())
                    .build();
            })
            .collect(Collectors.toList());

        return timeSlotRepository.saveAll(slots).stream()
            .map(SlotResponse::from)
            .collect(Collectors.toList());
    }

    public List<SlotResponse> getSlotsByMonth(Long sellerId, int year, int month) {
        LocalDateTime from = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime to = from.plusMonths(1);
        return timeSlotRepository.findBySellerIdAndPeriod(sellerId, from, to).stream()
            .map(SlotResponse::from)
            .collect(Collectors.toList());
    }

    public List<SlotResponse> getAvailableSlots(Long sellerId) {
        return timeSlotRepository.findAvailableBySellerIdFrom(
                sellerId, SlotStatus.AVAILABLE, LocalDateTime.now()).stream()
            .map(SlotResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional
    public void deleteSlot(Long sellerId, Long slotId) {
        TimeSlot slot = timeSlotRepository.findByIdAndSellerId(slotId, sellerId)
            .orElseThrow(() -> new TradevException(ErrorCode.SLOT_NOT_FOUND));

        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new TradevException(ErrorCode.SLOT_NOT_AVAILABLE);
        }
        timeSlotRepository.delete(slot);
    }

    // ───────────────────────────── Reservation ─────────────────────────────

    @Transactional
    public ReservationResponse createReservation(Long buyerId, ReservationRequest.Create request) {
        User buyer = getUser(buyerId);

        TimeSlot slot = timeSlotRepository.findById(request.slotId())
            .orElseThrow(() -> new TradevException(ErrorCode.SLOT_NOT_FOUND));

        if (slot.getSeller().getId().equals(buyerId)) {
            throw new TradevException(ErrorCode.SLOT_SELF_RESERVATION);
        }
        if (!slot.isAvailable()) {
            throw new TradevException(ErrorCode.SLOT_NOT_AVAILABLE);
        }
        // Redis 잠금 확인 (다른 사람이 임시 잠금 중)
        if (slotLockService.isLocked(slot.getId()) && !slotLockService.isLockedBy(slot.getId(), buyerId)) {
            throw new TradevException(ErrorCode.SLOT_LOCKED);
        }
        // 중복 예약 방지
        if (reservationRepository.existsBySlotIdAndBuyerIdAndStatusIn(
                slot.getId(), buyerId,
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED))) {
            throw new TradevException(ErrorCode.SLOT_ALREADY_RESERVED);
        }

        // 슬롯 상태 LOCKED, Redis 임시 잠금 해제
        slot.lock();
        slotLockService.unlock(slot.getId());

        Reservation reservation = Reservation.builder()
            .slot(slot)
            .buyer(buyer)
            .seller(slot.getSeller())
            .message(request.message())
            .build();

        Reservation saved = reservationRepository.save(reservation);
        // eventPublisher.publishEvent(new ReservationRequestedEvent(saved)); // Phase 2-4
        return ReservationResponse.from(saved);
    }

    @Transactional
    public ReservationResponse acceptReservation(Long sellerId, Long reservationId) {
        Reservation reservation = getReservationById(reservationId);

        if (!reservation.isSeller(sellerId)) {
            throw new TradevException(ErrorCode.SLOT_ACCESS_DENIED);
        }
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new TradevException(ErrorCode.SLOT_INVALID_STATUS);
        }

        reservation.accept();
        reservation.getSlot().reserve();

        // eventPublisher.publishEvent(new ReservationAcceptedEvent(reservation));
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse cancelReservation(Long userId, Long reservationId,
                                                   ReservationRequest.Cancel request) {
        Reservation reservation = getReservationById(reservationId);

        if (!reservation.isParticipant(userId)) {
            throw new TradevException(ErrorCode.SLOT_ACCESS_DENIED);
        }
        if (reservation.getStatus() == ReservationStatus.COMPLETED ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new TradevException(ErrorCode.SLOT_INVALID_STATUS);
        }

        reservation.cancel(request != null ? request.reason() : null);
        reservation.getSlot().unlock(); // 슬롯 다시 AVAILABLE

        // eventPublisher.publishEvent(new ReservationCancelledEvent(reservation));
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse completeReservation(Long userId, Long reservationId) {
        Reservation reservation = getReservationById(reservationId);

        if (!reservation.isParticipant(userId)) {
            throw new TradevException(ErrorCode.SLOT_ACCESS_DENIED);
        }
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new TradevException(ErrorCode.SLOT_INVALID_STATUS);
        }

        reservation.complete();
        // eventPublisher.publishEvent(new ReservationCompletedEvent(reservation));
        return ReservationResponse.from(reservation);
    }

    public ReservationResponse getReservation(Long userId, Long reservationId) {
        Reservation reservation = getReservationById(reservationId);
        if (!reservation.isParticipant(userId)) {
            throw new TradevException(ErrorCode.SLOT_ACCESS_DENIED);
        }
        return ReservationResponse.from(reservation);
    }

    public CursorPageResponse<ReservationResponse> getMyReservations(Long userId, String cursor,
                                                                       int size) {
        LocalDateTime cursorCreatedAt = null;
        Long cursorId = null;
        if (cursor != null) {
            String[] parts = cursor.split("\\|");
            if (parts.length == 2) {
                cursorCreatedAt = LocalDateTime.parse(parts[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                cursorId = Long.parseLong(parts[1]);
            }
        }

        List<Reservation> reservations = reservationRepository.findByUserWithCursor(
            userId, cursorCreatedAt, cursorId, size + 1
        );
        List<ReservationResponse> responses = reservations.stream()
            .map(ReservationResponse::from)
            .collect(Collectors.toList());

        return CursorPageResponse.of(responses, size,
            r -> r.createdAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "|" + r.id());
    }

    // ───────────────────────────── Private ─────────────────────────────

    private User getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new TradevException(ErrorCode.USER_NOT_FOUND));
    }

    private Reservation getReservationById(Long id) {
        return reservationRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new TradevException(ErrorCode.RESERVATION_NOT_FOUND));
    }

    private void validateSlotTime(LocalDateTime startedAt, LocalDateTime endedAt) {
        if (!endedAt.isAfter(startedAt)) {
            throw new TradevException(ErrorCode.SLOT_INVALID_TIME);
        }
        if (startedAt.isBefore(LocalDateTime.now())) {
            throw new TradevException(ErrorCode.SLOT_INVALID_TIME);
        }
    }
}
