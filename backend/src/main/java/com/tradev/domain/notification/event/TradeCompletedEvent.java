package com.tradev.domain.notification.event;

import com.tradev.domain.trade.entity.Trade;
import lombok.Getter;

@Getter
public class TradeCompletedEvent {
    private final Trade trade;

    public TradeCompletedEvent(Trade trade) {
        this.trade = trade;
    }
}
