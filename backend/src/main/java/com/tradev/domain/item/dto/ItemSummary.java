package com.tradev.domain.item.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ItemSummary {

    private Long id;
    private String title;
    private int price;
    private String status;
    private String thumbnailUrl;
    private int wishCount;
    private int viewCount;
    private String location;
    private String categoryName;
    private Long sellerId;
    private String sellerNickname;
    private LocalDateTime createdAt;
    private LocalDateTime boostedAt;
}
