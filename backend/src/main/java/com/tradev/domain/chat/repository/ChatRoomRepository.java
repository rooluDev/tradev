package com.tradev.domain.chat.repository;

import com.tradev.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByItemIdAndBuyerId(Long itemId, Long buyerId);

    @Query("""
        SELECT cr FROM ChatRoom cr
        JOIN FETCH cr.item i
        JOIN FETCH cr.buyer b
        JOIN FETCH cr.seller s
        WHERE (cr.buyer.id = :userId OR cr.seller.id = :userId)
          AND (:cursorCreatedAt IS NULL OR cr.updatedAt < :cursorCreatedAt
               OR (cr.updatedAt = :cursorCreatedAt AND cr.id < :cursorId))
        ORDER BY cr.updatedAt DESC, cr.id DESC
        LIMIT :pageSize
        """)
    List<ChatRoom> findByUserWithCursor(
        @Param("userId") Long userId,
        @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
        @Param("cursorId") Long cursorId,
        @Param("pageSize") int pageSize
    );

    @Query("""
        SELECT cr FROM ChatRoom cr
        JOIN FETCH cr.item
        JOIN FETCH cr.buyer
        JOIN FETCH cr.seller
        WHERE cr.id = :id
        """)
    Optional<ChatRoom> findByIdWithDetails(@Param("id") Long id);
}
