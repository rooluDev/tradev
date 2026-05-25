package com.tradev.domain.item.dto;

import com.tradev.domain.item.entity.ItemCondition;
import com.tradev.domain.item.entity.TradeType;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.util.List;

@Getter
public class ItemRequest {

    @NotNull
    private Long categoryId;

    @NotBlank
    @Size(min = 2, max = 100, message = "제목은 2~100자여야 합니다.")
    private String title;

    @NotBlank
    @Size(min = 10, max = 2000, message = "설명은 10~2000자여야 합니다.")
    private String description;

    @NotNull
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    @Max(value = 100_000_000, message = "가격은 1억원 이하여야 합니다.")
    private Integer price;

    private TradeType tradeType = TradeType.ALL;

    private ItemCondition itemCondition = ItemCondition.GOOD;

    private String location;

    @NotEmpty(message = "이미지를 최소 1장 등록해야 합니다.")
    @Size(max = 10, message = "이미지는 최대 10장까지 등록 가능합니다.")
    private List<String> imageS3Keys;
}
