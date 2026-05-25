package com.tradev.domain.item.repository;

import com.tradev.domain.item.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist> findByUserIdAndItemId(Long userId, Long itemId);

    boolean existsByUserIdAndItemId(Long userId, Long itemId);

    @Query("""
        SELECT w FROM Wishlist w
        JOIN FETCH w.item i
        JOIN FETCH i.seller
        WHERE w.user.id = :userId
          AND (:cursorCreatedAt IS NULL OR w.createdAt < :cursorCreatedAt
               OR (w.createdAt = :cursorCreatedAt AND w.id < :cursorId))
        ORDER BY w.createdAt DESC, w.id DESC
        LIMIT :pageSize
        """)
    List<Wishlist> findByUserIdWithCursor(
            @Param("userId") Long userId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            @Param("pageSize") int pageSize
    );
}
