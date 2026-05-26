package com.tradev.domain.report.controller;

import com.tradev.common.response.ApiResponse;
import com.tradev.domain.report.dto.ReportRequest;
import com.tradev.domain.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<Void> createReport(
        @AuthenticationPrincipal Long userId,
        @Valid @RequestBody ReportRequest request
    ) {
        reportService.createReport(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
