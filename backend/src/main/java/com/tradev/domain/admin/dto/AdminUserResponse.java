package com.tradev.domain.admin.dto;

import com.tradev.domain.user.entity.User;

import java.time.LocalDateTime;

public record AdminUserResponse(
    Long id,
    String email,
    String nickname,
    String role,
    String status,
    int trustScore,
    String trustGrade,
    LocalDateTime createdAt,
    LocalDateTime suspendedUntil
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getRole().name(),
            user.getStatus().name(),
            user.getTrustScore(),
            user.getTrustGrade().name(),
            user.getCreatedAt(),
            user.getSuspendedUntil()
        );
    }
}
