package com.tradev.domain.review.dto;

import com.tradev.domain.review.entity.Review;

import java.time.LocalDateTime;

public record ReviewResponse(
    Long id,
    Long tradeId,
    UserInfo reviewer,
    int rating,
    String content,
    String reply,
    LocalDateTime createdAt
) {
    public record UserInfo(Long id, String nickname, String profileImageUrl) {}

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
            review.getId(),
            review.getTrade().getId(),
            new UserInfo(
                review.getReviewer().getId(),
                review.getReviewer().getNickname(),
                review.getReviewer().getProfileImageUrl()
            ),
            review.getRating(),
            review.getContent(),
            review.getReply(),
            review.getCreatedAt()
        );
    }
}
