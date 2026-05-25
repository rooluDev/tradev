package com.tradev.domain.review.dto;

import com.tradev.common.dto.CursorPageResponse;

public record ReviewSummaryResponse(
    double averageRating,
    long totalCount,
    CursorPageResponse<ReviewResponse> reviews
) {}
