package com.tradev.domain.trade.entity;

import com.tradev.common.entity.BaseTimeEntity;
import com.tradev.domain.item.entity.Item;
import com.tradev.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trade extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TradeStatus status = TradeStatus.PENDING;

    @Column(nullable = false)
    private int price;

    private Boolean buyerConfirmed = false;
    private Boolean sellerConfirmed = false;

    @Column(length = 500)
    private String cancelReason;

    private LocalDateTime completedAt;

    @Version
    private Long version;

    @Builder
    public Trade(Item item, User buyer, User seller, int price) {
        this.item = item;
        this.buyer = buyer;
        this.seller = seller;
        this.price = price;
    }

    public void accept() {
        this.status = TradeStatus.RESERVED;
    }

    public void reject() {
        this.status = TradeStatus.REJECTED;
    }

    public void cancel(String reason) {
        this.status = TradeStatus.CANCELLED;
        this.cancelReason = reason;
    }

    public boolean confirm(boolean isBuyer) {
        if (isBuyer) this.buyerConfirmed = true;
        else this.sellerConfirmed = true;

        if (Boolean.TRUE.equals(this.buyerConfirmed) && Boolean.TRUE.equals(this.sellerConfirmed)) {
            this.status = TradeStatus.COMPLETED;
            this.completedAt = LocalDateTime.now();
            return true;
        }
        return false;
    }

    public boolean isBuyer(Long userId) {
        return buyer.getId().equals(userId);
    }

    public boolean isSeller(Long userId) {
        return seller.getId().equals(userId);
    }

    public boolean isParticipant(Long userId) {
        return isBuyer(userId) || isSeller(userId);
    }
}
