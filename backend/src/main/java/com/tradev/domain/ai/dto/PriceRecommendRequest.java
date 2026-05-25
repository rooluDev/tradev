package com.tradev.domain.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record PriceRecommendRequest(
    @NotBlank String title,
    @NotBlank String categoryName,
    String description
) {}
