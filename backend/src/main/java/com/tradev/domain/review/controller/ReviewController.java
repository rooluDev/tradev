package com.tradev.domain.review.controller;

import com.tradev.common.response.ApiResponse;
import com.tradev.domain.review.dto.ReviewRequest;
import com.tradev.domain.review.dto.ReviewResponse;
import com.tradev.domain.review.dto.ReviewSummaryResponse;
import com.tradev.domain.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** 리뷰 작성 */
    @PostMapping("/api/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
        @AuthenticationPrincipal Long userId,
        @Valid @RequestBody ReviewRequest.Create request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(reviewService.createReview(userId, request)));
    }

    /** 사용자 리뷰 목록 (평균 별점 포함) */
    @GetMapping("/api/users/{userId}/reviews")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> getReviews(
        @PathVariable Long userId,
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(reviewService.getReviews(userId, cursor, size))
        );
    }

    /** 리뷰 답글 작성 */
    @PostMapping("/api/reviews/{reviewId}/reply")
    public ResponseEntity<ApiResponse<ReviewResponse>> addReply(
        @AuthenticationPrincipal Long userId,
        @PathVariable Long reviewId,
        @Valid @RequestBody ReviewRequest.Reply request
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(reviewService.addReply(userId, reviewId, request))
        );
    }
}
