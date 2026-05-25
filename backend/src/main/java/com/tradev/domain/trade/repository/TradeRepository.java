package com.tradev.domain.trade.repository;

import com.tradev.domain.trade.entity.Trade;
import com.tradev.domain.trade.entity.TradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    // 진행 중 중복 요청 방지
    boolean existsByItemIdAndBuyerIdAndStatusIn(Long itemId, Long buyerId, List<TradeStatus> statuses);

    @Query("""
        SELECT t FROM Trade t
        JOIN FETCH t.item i
        JOIN FETCH t.buyer b
        JOIN FETCH t.seller s
        WHERE (t.buyer.id = :userId OR t.seller.id = :userId)
          AND (:status IS NULL OR t.status = :status)
          AND (:cursorCreatedAt IS NULL OR t.createdAt < :cursorCreatedAt
               OR (t.createdAt = :cursorCreatedAt AND t.id < :cursorId))
        ORDER BY t.createdAt DESC, t.id DESC
        LIMIT :pageSize
        """)
    List<Trade> findByUserWithCursor(
            @Param("userId") Long userId,
            @Param("status") TradeStatus status,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            @Param("pageSize") int pageSize
    );

    @Query("""
        SELECT t FROM Trade t
        JOIN FETCH t.item
        JOIN FETCH t.buyer
        JOIN FETCH t.seller
        WHERE t.id = :id
        """)
    Optional<Trade> findByIdWithDetails(@Param("id") Long id);

    long countByCreatedAtAfter(LocalDateTime after);
}
