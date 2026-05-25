package com.tradev.domain.report.dto;

import com.tradev.domain.report.entity.ReportReason;
import com.tradev.domain.report.entity.TargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(
    @NotNull TargetType targetType,
    @NotNull Long targetId,
    @NotNull ReportReason reason,
    @Size(max = 500) String detail
) {}
