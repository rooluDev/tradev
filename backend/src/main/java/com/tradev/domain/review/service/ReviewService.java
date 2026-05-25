package com.tradev.domain.review.service;

import com.tradev.common.dto.CursorPageResponse;
import com.tradev.common.exception.ErrorCode;
import com.tradev.common.exception.TradevException;
import com.tradev.domain.notification.entity.NotificationType;
import com.tradev.domain.notification.service.NotificationService;
import com.tradev.domain.review.dto.ReviewRequest;
import com.tradev.domain.review.dto.ReviewResponse;
import com.tradev.domain.review.dto.ReviewSummaryResponse;
import com.tradev.domain.review.entity.Review;
import com.tradev.domain.review.repository.ReviewRepository;
import com.tradev.domain.trade.entity.Trade;
import com.tradev.domain.trade.entity.TradeStatus;
import com.tradev.domain.trade.repository.TradeRepository;
import com.tradev.domain.user.entity.User;
import com.tradev.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public ReviewResponse createReview(Long reviewerId, ReviewRequest.Create request) {
        Trade trade = tradeRepository.findByIdWithDetails(request.tradeId())
            .orElseThrow(() -> new TradevException(ErrorCode.TRADE_NOT_FOUND));

        if (trade.getStatus() != TradeStatus.COMPLETED) {
            throw new TradevException(ErrorCode.REVIEW_TRADE_NOT_COMPLETED);
        }
        if (!trade.isParticipant(reviewerId)) {
            throw new TradevException(ErrorCode.REVIEW_NOT_ALLOWED);
        }
        if (reviewRepository.existsByTradeIdAndReviewerId(request.tradeId(), reviewerId)) {
            throw new TradevException(ErrorCode.REVIEW_DUPLICATE);
        }
        if (trade.getCompletedAt() != null &&
            trade.getCompletedAt().plusDays(7).isBefore(LocalDateTime.now())) {
            throw new TradevException(ErrorCode.REVIEW_PERIOD_EXPIRED);
        }

        User reviewer = userRepository.findById(reviewerId)
            .orElseThrow(() -> new TradevException(ErrorCode.USER_NOT_FOUND));
        User reviewee = trade.isBuyer(reviewerId) ? trade.getSeller() : trade.getBuyer();

        Review review = Review.builder()
            .trade(trade)
            .reviewer(reviewer)
            .reviewee(reviewee)
            .rating(request.rating())
            .content(request.content())
            .build();
        reviewRepository.save(review);

        // 신뢰 점수 +2 (작성자)
        reviewer.addTrustScore(2);

        // 피작성자 알림
        notificationService.createAndSend(
            reviewee.getId(),
            NotificationType.REVIEW_RECEIVED,
            String.format("%s님이 리뷰를 남겼습니다.", reviewer.getNickname()),
            "/users/" + reviewee.getId()
        );

        return ReviewResponse.from(review);
    }

    public ReviewSummaryResponse getReviews(Long revieweeId, String cursor, int size) {
        LocalDateTime cursorCreatedAt = null;
        Long cursorId = null;
        if (cursor != null) {
            String[] parts = cursor.split("\\|");
            if (parts.length == 2) {
                cursorCreatedAt = LocalDateTime.parse(parts[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                cursorId = Long.parseLong(parts[1]);
            }
        }

        List<Review> reviews = reviewRepository.findByRevieweeWithCursor(
            revieweeId, cursorCreatedAt, cursorId, size + 1
        );
        double avg = reviewRepository.findAverageRatingByRevieweeId(revieweeId).orElse(0.0);
        long total = reviewRepository.count(); // TODO: count by revieweeId

        List<ReviewResponse> responses = reviews.stream()
            .map(ReviewResponse::from)
            .collect(Collectors.toList());

        CursorPageResponse<ReviewResponse> page = CursorPageResponse.of(responses, size,
            r -> r.createdAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "|" + r.id());

        return new ReviewSummaryResponse(
            Math.round(avg * 10.0) / 10.0,
            total,
            page
        );
    }

    @Transactional
    public ReviewResponse addReply(Long userId, Long reviewId, ReviewRequest.Reply request) {
        Review review = reviewRepository.findByIdWithDetails(reviewId)
            .orElseThrow(() -> new TradevException(ErrorCode.REVIEW_NOT_FOUND));

        // 피작성자(reviewee)만 답글 가능
        if (!review.getTrade().isParticipant(userId) ||
            review.getReviewer().getId().equals(userId)) {
            throw new TradevException(ErrorCode.REVIEW_NOT_ALLOWED);
        }
        if (review.hasReply()) {
            throw new TradevException(ErrorCode.REVIEW_REPLY_DUPLICATE);
        }

        review.addReply(request.reply());
        return ReviewResponse.from(review);
    }
}
