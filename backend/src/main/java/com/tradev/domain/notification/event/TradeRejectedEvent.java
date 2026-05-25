package com.tradev.domain.notification.event;

import com.tradev.domain.trade.entity.Trade;
import lombok.Getter;

@Getter
public class TradeRejectedEvent {
    private final Trade trade;

    public TradeRejectedEvent(Trade trade) {
        this.trade = trade;
    }
}
