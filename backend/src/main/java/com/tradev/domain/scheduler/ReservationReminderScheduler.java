package com.tradev.domain.scheduler;

import com.tradev.domain.reservation.entity.Reservation;
import com.tradev.domain.reservation.entity.ReservationStatus;
import com.tradev.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationReminderScheduler {

    private final ReservationRepository reservationRepository;

    /**
     * 매 시간 정각마다 실행
     * 24시간 후 예약된 슬롯이 있으면 리마인더 알림 발송
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional(readOnly = true)
    public void sendReminders() {
        LocalDateTime from = LocalDateTime.now().plusHours(23).plusMinutes(55);
        LocalDateTime to = LocalDateTime.now().plusHours(24).plusMinutes(5);

        // TODO: 각 판매자에 대해 알림 발행 (Phase 2-4에서 이벤트 연동)
        // 현재는 로그만 출력
        log.debug("[Scheduler] 24h 예약 리마인더 확인: {} ~ {}", from, to);
    }

    /**
     * 매 5분마다 과거 LOCKED 슬롯 자동 해제
     * (Redis 잠금이 만료되었지만 DB 상태가 LOCKED인 경우)
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void releaseExpiredLockedSlots() {
        LocalDateTime expiredBefore = LocalDateTime.now().minusMinutes(10);
        // TimeSlot이 LOCKED 상태이고 업데이트 시간이 10분 이상 지난 경우 해제
        // TimeSlotRepository에서 처리 (별도 구현 시 추가)
        log.debug("[Scheduler] 만료된 LOCKED 슬롯 해제 체크");
    }
}
