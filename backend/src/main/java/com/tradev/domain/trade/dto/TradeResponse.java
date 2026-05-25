package com.tradev.domain.trade.dto;

import com.tradev.domain.trade.entity.Trade;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class TradeResponse {

    private final Long id;
    private final String status;
    private final int price;
    private final boolean buyerConfirmed;
    private final boolean sellerConfirmed;
    private final String cancelReason;
    private final LocalDateTime completedAt;
    private final LocalDateTime createdAt;
    private final ItemInfo item;
    private final UserInfo buyer;
    private final UserInfo seller;

    public TradeResponse(Trade trade) {
        this.id = trade.getId();
        this.status = trade.getStatus().name();
        this.price = trade.getPrice();
        this.buyerConfirmed = Boolean.TRUE.equals(trade.getBuyerConfirmed());
        this.sellerConfirmed = Boolean.TRUE.equals(trade.getSellerConfirmed());
        this.cancelReason = trade.getCancelReason();
        this.completedAt = trade.getCompletedAt();
        this.createdAt = trade.getCreatedAt();
        this.item = new ItemInfo(
                trade.getItem().getId(),
                trade.getItem().getTitle(),
                trade.getItem().getImages().isEmpty() ? null : trade.getItem().getImages().get(0).getImageUrl()
        );
        this.buyer = new UserInfo(trade.getBuyer().getId(), trade.getBuyer().getNickname(), trade.getBuyer().getProfileImageUrl());
        this.seller = new UserInfo(trade.getSeller().getId(), trade.getSeller().getNickname(), trade.getSeller().getProfileImageUrl());
    }

    public record ItemInfo(Long id, String title, String thumbnailUrl) {}
    public record UserInfo(Long id, String nickname, String profileImageUrl) {}
}
