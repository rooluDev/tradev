package com.tradev.domain.admin.service;

import com.tradev.common.dto.CursorPageResponse;
import com.tradev.common.exception.ErrorCode;
import com.tradev.common.exception.TradevException;
import com.tradev.domain.admin.dto.AdminDashboardResponse;
import com.tradev.domain.admin.dto.AdminReportResponse;
import com.tradev.domain.admin.dto.AdminUserResponse;
import com.tradev.domain.ai.client.ClaudeWebClient;
import com.tradev.domain.item.repository.ItemRepository;
import com.tradev.domain.report.entity.Report;
import com.tradev.domain.report.entity.ReportStatus;
import com.tradev.domain.report.repository.ReportRepository;
import com.tradev.domain.trade.repository.TradeRepository;
import com.tradev.domain.user.entity.User;
import com.tradev.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final TradeRepository tradeRepository;
    private final ReportRepository reportRepository;
    private final ClaudeWebClient claudeWebClient;

    // ──────────── Dashboard ────────────

    public AdminDashboardResponse getDashboard() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        return new AdminDashboardResponse(
            userRepository.countByCreatedAtAfter(todayStart),
            tradeRepository.countByCreatedAtAfter(todayStart),
            reportRepository.countByStatus(ReportStatus.PENDING),
            itemRepository.count(),
            userRepository.count(),
            tradeRepository.count()
        );
    }

    // ──────────── Users ────────────

    public CursorPageResponse<AdminUserResponse> getUsers(String cursor, int size) {
        LocalDateTime cursorTime = null;
        Long cursorId = null;
        if (cursor != null) {
            String[] parts = cursor.split("\\|");
            if (parts.length == 2) {
                cursorTime = LocalDateTime.parse(parts[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                cursorId = Long.parseLong(parts[1]);
            }
        }
        List<User> users = userRepository.findAllWithCursor(cursorTime, cursorId, size + 1);
        List<AdminUserResponse> responses = users.stream()
            .map(AdminUserResponse::from)
            .collect(Collectors.toList());
        return CursorPageResponse.of(responses, size,
            r -> r.createdAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "|" + r.id());
    }

    public AdminUserResponse getUser(Long userId) {
        return AdminUserResponse.from(getUserEntity(userId));
    }

    @Transactional
    public AdminUserResponse suspendUser(Long userId, int days) {
        User user = getUserEntity(userId);
        user.suspend(LocalDateTime.now().plusDays(days));
        user.addTrustScore(-10);
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse activateUser(Long userId) {
        User user = getUserEntity(userId);
        user.activate();
        return AdminUserResponse.from(user);
    }

    // ──────────── Reports ────────────

    public CursorPageResponse<AdminReportResponse> getReports(String statusStr, String cursor, int size) {
        ReportStatus status = statusStr != null ? ReportStatus.valueOf(statusStr) : ReportStatus.PENDING;
        LocalDateTime cursorTime = null;
        Long cursorId = null;
        if (cursor != null) {
            String[] parts = cursor.split("\\|");
            if (parts.length == 2) {
                cursorTime = LocalDateTime.parse(parts[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                cursorId = Long.parseLong(parts[1]);
            }
        }
        List<Report> reports = reportRepository.findByStatusWithCursor(status, cursorTime, cursorId, size + 1);
        List<AdminReportResponse> responses = reports.stream()
            .map(AdminReportResponse::from)
            .collect(Collectors.toList());
        return CursorPageResponse.of(responses, size,
            r -> r.createdAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "|" + r.id());
    }

    @Transactional
    public AdminReportResponse processReport(Long reportId, ReportStatus decision, String adminNote) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new TradevException(ErrorCode.RESOURCE_NOT_FOUND));
        report.process(decision, adminNote);
        return AdminReportResponse.from(report);
    }

    /**
     * 신고 내용 AI 요약 (Claude API)
     */
    public Mono<String> summarizeReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new TradevException(ErrorCode.RESOURCE_NOT_FOUND));

        String prompt = String.format(
            "다음 신고 내용을 3줄로 요약해주세요:\n신고 대상 유형: %s\n신고 사유: %s\n상세 내용: %s",
            report.getTargetType().name(),
            report.getReason().name(),
            report.getDetail() != null ? report.getDetail() : "(없음)"
        );

        return claudeWebClient.complete("신고 내용을 간결하게 요약하는 도우미입니다.", prompt);
    }

    // ──────────── Private ────────────

    private User getUserEntity(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new TradevException(ErrorCode.USER_NOT_FOUND));
    }
}
