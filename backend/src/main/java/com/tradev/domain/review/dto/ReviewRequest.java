package com.tradev.domain.review.dto;

import jakarta.validation.constraints.*;

public class ReviewRequest {

    public record Create(
        @NotNull(message = "거래 ID는 필수입니다")
        Long tradeId,

        @NotNull @Min(1) @Max(5)
        Integer rating,

        @NotBlank(message = "리뷰 내용은 필수입니다")
        @Size(max = 500)
        String content
    ) {}

    public record Reply(
        @NotBlank(message = "답글 내용은 필수입니다")
        @Size(max = 500)
        String reply
    ) {}
}
