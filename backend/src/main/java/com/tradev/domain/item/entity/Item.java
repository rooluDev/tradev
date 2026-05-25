package com.tradev.domain.item.entity;

import com.tradev.common.entity.BaseTimeEntity;
import com.tradev.domain.category.entity.Category;
import com.tradev.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE items SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Item extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TradeType tradeType = TradeType.ALL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemStatus status = ItemStatus.SALE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemCondition itemCondition = ItemCondition.GOOD;

    @Column(nullable = false)
    private boolean hidden = false;

    @Column(nullable = false)
    private int viewCount = 0;

    @Column(nullable = false)
    private int wishCount = 0;

    @Column(length = 100)
    private String location;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ItemImage> images = new ArrayList<>();

    private java.time.LocalDateTime deletedAt;
    private java.time.LocalDateTime boostedAt;

    @Builder
    public Item(User seller, Category category, String title, String description,
                int price, TradeType tradeType, ItemCondition itemCondition, String location) {
        this.seller = seller;
        this.category = category;
        this.title = title;
        this.description = description;
        this.price = price;
        this.tradeType = tradeType != null ? tradeType : TradeType.ALL;
        this.itemCondition = itemCondition != null ? itemCondition : ItemCondition.GOOD;
        this.location = location;
    }

    public void update(Category category, String title, String description,
                       int price, TradeType tradeType, ItemCondition itemCondition, String location) {
        this.category = category;
        this.title = title;
        this.description = description;
        this.price = price;
        this.tradeType = tradeType;
        this.itemCondition = itemCondition;
        this.location = location;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public void toggleHidden() {
        this.hidden = !this.hidden;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void addWish() {
        this.wishCount++;
    }

    public void removeWish() {
        if (this.wishCount > 0) this.wishCount--;
    }

    public void boost() {
        this.boostedAt = java.time.LocalDateTime.now();
    }

    public boolean isOwner(Long userId) {
        return seller.getId().equals(userId);
    }

    /** 첫 번째 이미지 URL (썸네일용) */
    public String getThumbnailUrl() {
        if (images == null || images.isEmpty()) return null;
        return images.stream()
            .min(java.util.Comparator.comparingInt(ItemImage::getSortOrder))
            .map(ItemImage::getImageUrl)
            .orElse(null);
    }
}
