package com.tradev.domain.trade.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class TradeRequest {

    @NotNull
    private Long itemId;

    private String message;
}
