package com.tradev.domain.reservation.repository;

import com.tradev.domain.reservation.entity.SlotStatus;
import com.tradev.domain.reservation.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    @Query("SELECT ts FROM TimeSlot ts WHERE ts.seller.id = :sellerId " +
           "AND ts.startedAt >= :from AND ts.startedAt < :to " +
           "ORDER BY ts.startedAt ASC")
    List<TimeSlot> findBySellerIdAndPeriod(@Param("sellerId") Long sellerId,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    @Query("SELECT ts FROM TimeSlot ts WHERE ts.seller.id = :sellerId " +
           "AND ts.status = :status AND ts.startedAt >= :from " +
           "ORDER BY ts.startedAt ASC")
    List<TimeSlot> findAvailableBySellerIdFrom(@Param("sellerId") Long sellerId,
                                               @Param("status") SlotStatus status,
                                               @Param("from") LocalDateTime from);

    Optional<TimeSlot> findByIdAndSellerId(Long id, Long sellerId);

    @Query("SELECT ts FROM TimeSlot ts WHERE ts.status = :status " +
           "AND ts.startedAt < :before")
    List<TimeSlot> findExpiredLockedSlots(@Param("status") SlotStatus status,
                                          @Param("before") LocalDateTime before);

    boolean existsBySellerIdAndStartedAt(Long sellerId, LocalDateTime startedAt);
}
