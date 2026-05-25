package com.tradev.domain.reservation.repository;

import com.tradev.domain.reservation.entity.Reservation;
import com.tradev.domain.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
        SELECT r FROM Reservation r
        JOIN FETCH r.slot
        JOIN FETCH r.buyer
        JOIN FETCH r.seller
        WHERE r.id = :id
        """)
    Optional<Reservation> findByIdWithDetails(@Param("id") Long id);

    @Query("""
        SELECT r FROM Reservation r
        JOIN FETCH r.slot
        JOIN FETCH r.buyer
        JOIN FETCH r.seller
        WHERE (r.buyer.id = :userId OR r.seller.id = :userId)
          AND (:cursorCreatedAt IS NULL OR r.createdAt < :cursorCreatedAt
               OR (r.createdAt = :cursorCreatedAt AND r.id < :cursorId))
        ORDER BY r.createdAt DESC, r.id DESC
        LIMIT :pageSize
        """)
    List<Reservation> findByUserWithCursor(
        @Param("userId") Long userId,
        @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
        @Param("cursorId") Long cursorId,
        @Param("pageSize") int pageSize
    );

    @Query("SELECT r FROM Reservation r WHERE r.slot.id = :slotId " +
           "AND r.status IN :statuses")
    List<Reservation> findBySlotIdAndStatusIn(@Param("slotId") Long slotId,
                                               @Param("statuses") List<ReservationStatus> statuses);

    @Query("""
        SELECT r FROM Reservation r
        JOIN FETCH r.slot
        JOIN FETCH r.buyer
        WHERE r.seller.id = :sellerId
          AND r.status = :status
          AND r.slot.startedAt BETWEEN :from AND :to
        """)
    List<Reservation> findUpcomingBySeller(@Param("sellerId") Long sellerId,
                                            @Param("status") ReservationStatus status,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    boolean existsBySlotIdAndBuyerIdAndStatusIn(Long slotId, Long buyerId,
                                                 List<ReservationStatus> statuses);
}
