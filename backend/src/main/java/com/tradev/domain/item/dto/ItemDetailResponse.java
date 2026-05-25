package com.tradev.domain.item.dto;

import com.tradev.domain.item.entity.Item;
import com.tradev.domain.item.entity.ItemImage;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class ItemDetailResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final int price;
    private final String status;
    private final String tradeType;
    private final String itemCondition;
    private final String location;
    private final int viewCount;
    private final int wishCount;
    private final boolean hidden;
    private final List<ImageInfo> images;
    private final SellerInfo seller;
    private final CategoryInfo category;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private boolean wishedByMe = false;

    public ItemDetailResponse(Item item) {
        this.id = item.getId();
        this.title = item.getTitle();
        this.description = item.getDescription();
        this.price = item.getPrice();
        this.status = item.getStatus().name();
        this.tradeType = item.getTradeType().name();
        this.itemCondition = item.getItemCondition().name();
        this.location = item.getLocation();
        this.viewCount = item.getViewCount();
        this.wishCount = item.getWishCount();
        this.hidden = item.isHidden();
        this.images = item.getImages().stream()
                .map(img -> new ImageInfo(img.getId(), img.getImageUrl(), img.getSortOrder()))
                .toList();
        this.seller = new SellerInfo(
                item.getSeller().getId(),
                item.getSeller().getNickname(),
                item.getSeller().getProfileImageUrl(),
                item.getSeller().getTrustGrade().name(),
                item.getSeller().getTrustGrade().getIcon()
        );
        this.category = new CategoryInfo(
                item.getCategory().getId(),
                item.getCategory().getName()
        );
        this.createdAt = item.getCreatedAt();
        this.updatedAt = item.getUpdatedAt();
    }

    public void setWishedByMe(boolean wishedByMe) {
        this.wishedByMe = wishedByMe;
    }

    public record ImageInfo(Long id, String imageUrl, int sortOrder) {}
    public record SellerInfo(Long id, String nickname, String profileImageUrl, String trustGrade, String trustGradeIcon) {}
    public record CategoryInfo(Long id, String name) {}
}
