package com.tradev.domain.admin.dto;

public record AdminDashboardResponse(
    long todaySignups,
    long todayTrades,
    long pendingReports,
    long totalItems,
    long totalUsers,
    long totalTrades
) {}
