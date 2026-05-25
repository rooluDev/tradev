package com.tradev.domain.review.entity;

import com.tradev.common.entity.BaseTimeEntity;
import com.tradev.domain.trade.entity.Trade;
import com.tradev.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reviews",
    uniqueConstraints = @UniqueConstraint(columnNames = {"trade_id", "reviewer_id"}),
    indexes = {
        @Index(name = "idx_review_reviewee", columnList = "reviewee_id"),
        @Index(name = "idx_review_trade", columnList = "trade_id")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false)
    private Trade trade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_id", nullable = false)
    private User reviewee;

    @Column(nullable = false)
    private int rating;  // 1~5

    @Column(nullable = false, length = 500)
    private String content;

    @Column(length = 500)
    private String reply;  // 피작성자 답글

    @Builder
    public Review(Trade trade, User reviewer, User reviewee, int rating, String content) {
        this.trade = trade;
        this.reviewer = reviewer;
        this.reviewee = reviewee;
        this.rating = rating;
        this.content = content;
    }

    public void addReply(String reply) {
        this.reply = reply;
    }

    public boolean hasReply() {
        return this.reply != null && !this.reply.isBlank();
    }
}
