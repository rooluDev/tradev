package com.tradev.domain.report.entity;

import com.tradev.common.entity.BaseTimeEntity;
import com.tradev.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reports",
    uniqueConstraints = @UniqueConstraint(columnNames = {"reporter_id", "target_type", "target_id"}),
    indexes = {
        @Index(name = "idx_report_status", columnList = "status"),
        @Index(name = "idx_report_target", columnList = "target_type, target_id")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportReason reason;

    @Column(length = 500)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(length = 500)
    private String adminNote;

    @Builder
    public Report(User reporter, TargetType targetType, Long targetId,
                  ReportReason reason, String detail) {
        this.reporter = reporter;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.detail = detail;
    }

    public void process(ReportStatus status, String adminNote) {
        this.status = status;
        this.adminNote = adminNote;
    }
}
