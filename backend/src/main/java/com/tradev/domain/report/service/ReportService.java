package com.tradev.domain.report.service;

import com.tradev.common.exception.ErrorCode;
import com.tradev.common.exception.TradevException;
import com.tradev.domain.report.dto.ReportRequest;
import com.tradev.domain.report.entity.Report;
import com.tradev.domain.report.repository.ReportRepository;
import com.tradev.domain.user.entity.User;
import com.tradev.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createReport(Long reporterId, ReportRequest request) {
        // 자기 자신 신고 방지 (USER 타입일 경우)
        if (request.targetType().name().equals("USER") &&
            request.targetId().equals(reporterId)) {
            throw new TradevException(ErrorCode.REPORT_SELF_NOT_ALLOWED);
        }

        // 중복 신고 방지
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                reporterId, request.targetType(), request.targetId())) {
            throw new TradevException(ErrorCode.REPORT_DUPLICATE);
        }

        User reporter = userRepository.findById(reporterId)
            .orElseThrow(() -> new TradevException(ErrorCode.USER_NOT_FOUND));

        Report report = Report.builder()
            .reporter(reporter)
            .targetType(request.targetType())
            .targetId(request.targetId())
            .reason(request.reason())
            .detail(request.detail())
            .build();

        reportRepository.save(report);
    }
}
