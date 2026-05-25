package com.tradev.domain.admin.controller;

import com.tradev.common.dto.CursorPageResponse;
import com.tradev.common.response.ApiResponse;
import com.tradev.domain.admin.dto.AdminDashboardResponse;
import com.tradev.domain.admin.dto.AdminReportResponse;
import com.tradev.domain.admin.dto.AdminUserResponse;
import com.tradev.domain.admin.service.AdminService;
import com.tradev.domain.report.entity.ReportStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /** 대시보드 통계 */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboard()));
    }

    /** 회원 목록 */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<CursorPageResponse<AdminUserResponse>>> getUsers(
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getUsers(cursor, size)));
    }

    /** 회원 상세 */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getUser(userId)));
    }

    /** 회원 정지 */
    @PatchMapping("/users/{userId}/suspend")
    public ResponseEntity<ApiResponse<AdminUserResponse>> suspendUser(
        @PathVariable Long userId,
        @RequestParam(defaultValue = "7") int days
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminService.suspendUser(userId, days)));
    }

    /** 회원 정지 해제 */
    @PatchMapping("/users/{userId}/activate")
    public ResponseEntity<ApiResponse<AdminUserResponse>> activateUser(
        @PathVariable Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminService.activateUser(userId)));
    }

    /** 신고 목록 */
    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<CursorPageResponse<AdminReportResponse>>> getReports(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getReports(status, cursor, size)));
    }

    /** 신고 처리 (수락/기각) */
    @PostMapping("/reports/{reportId}/process")
    public ResponseEntity<ApiResponse<AdminReportResponse>> processReport(
        @PathVariable Long reportId,
        @RequestParam ReportStatus decision,
        @RequestParam(required = false) String adminNote
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            adminService.processReport(reportId, decision, adminNote)
        ));
    }

    /** 신고 내용 AI 요약 */
    @GetMapping(value = "/reports/{reportId}/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ApiResponse<String>>> summarizeReport(
        @PathVariable Long reportId
    ) {
        return adminService.summarizeReport(reportId)
            .map(summary -> ResponseEntity.ok(ApiResponse.success(summary)));
    }
}
