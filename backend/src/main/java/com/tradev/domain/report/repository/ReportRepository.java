package com.tradev.domain.report.repository;

import com.tradev.domain.report.entity.Report;
import com.tradev.domain.report.entity.ReportStatus;
import com.tradev.domain.report.entity.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(
        Long reporterId, TargetType targetType, Long targetId
    );

    @Query("""
        SELECT r FROM Report r
        JOIN FETCH r.reporter
        WHERE r.status = :status
          AND (:cursorCreatedAt IS NULL OR r.createdAt < :cursorCreatedAt
               OR (r.createdAt = :cursorCreatedAt AND r.id < :cursorId))
        ORDER BY r.createdAt DESC, r.id DESC
        LIMIT :pageSize
        """)
    List<Report> findByStatusWithCursor(
        @Param("status") ReportStatus status,
        @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
        @Param("cursorId") Long cursorId,
        @Param("pageSize") int pageSize
    );

    long countByStatus(ReportStatus status);
}
