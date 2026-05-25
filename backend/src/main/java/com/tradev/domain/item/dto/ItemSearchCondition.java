package com.tradev.domain.item.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ItemSearchCondition {

    private String keyword;
    private Long categoryId;
    private Integer minPrice;
    private Integer maxPrice;
    private String tradeType;
    private String itemCondition;
    private String status;
    private Long sellerId;

    // cursor pagination
    private LocalDateTime cursorCreatedAt;
    private Long cursorId;

    @Builder.Default
    private int pageSize = 20;
}
