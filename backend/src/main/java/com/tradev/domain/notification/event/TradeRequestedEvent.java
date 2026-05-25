package com.tradev.domain.notification.event;

import com.tradev.domain.trade.entity.Trade;
import lombok.Getter;

@Getter
public class TradeRequestedEvent {
    private final Trade trade;

    public TradeRequestedEvent(Trade trade) {
        this.trade = trade;
    }
}
