package com.tradev.domain.review.repository;

import com.tradev.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByTradeIdAndReviewerId(Long tradeId, Long reviewerId);

    @Query("""
        SELECT r FROM Review r
        JOIN FETCH r.reviewer
        WHERE r.reviewee.id = :revieweeId
          AND (:cursorCreatedAt IS NULL OR r.createdAt < :cursorCreatedAt
               OR (r.createdAt = :cursorCreatedAt AND r.id < :cursorId))
        ORDER BY r.createdAt DESC, r.id DESC
        LIMIT :pageSize
        """)
    List<Review> findByRevieweeWithCursor(
        @Param("revieweeId") Long revieweeId,
        @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
        @Param("cursorId") Long cursorId,
        @Param("pageSize") int pageSize
    );

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewee.id = :revieweeId")
    Optional<Double> findAverageRatingByRevieweeId(@Param("revieweeId") Long revieweeId);

    @Query("""
        SELECT r FROM Review r
        JOIN FETCH r.reviewer
        JOIN FETCH r.trade
        WHERE r.id = :id
        """)
    Optional<Review> findByIdWithDetails(@Param("id") Long id);
}
