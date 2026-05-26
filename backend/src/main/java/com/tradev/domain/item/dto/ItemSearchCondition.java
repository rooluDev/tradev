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

    // MyBatis LIMIT 파라미터 산술 연산 불가 → pageSize + 1 계산값을 별도 제공
    public int getLimitSize() {
        return pageSize + 1;
    }
}
