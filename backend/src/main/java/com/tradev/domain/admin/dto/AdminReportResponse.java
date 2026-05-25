package com.tradev.domain.admin.dto;

import com.tradev.domain.report.entity.Report;
import com.tradev.domain.report.entity.ReportReason;
import com.tradev.domain.report.entity.ReportStatus;
import com.tradev.domain.report.entity.TargetType;

import java.time.LocalDateTime;

public record AdminReportResponse(
    Long id,
    String reporterNickname,
    TargetType targetType,
    Long targetId,
    ReportReason reason,
    String detail,
    ReportStatus status,
    String adminNote,
    LocalDateTime createdAt
) {
    public static AdminReportResponse from(Report report) {
        return new AdminReportResponse(
            report.getId(),
            report.getReporter().getNickname(),
            report.getTargetType(),
            report.getTargetId(),
            report.getReason(),
            report.getDetail(),
            report.getStatus(),
            report.getAdminNote(),
            report.getCreatedAt()
        );
    }
}
