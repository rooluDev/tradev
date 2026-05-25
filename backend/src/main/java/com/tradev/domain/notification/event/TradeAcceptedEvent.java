package com.tradev.domain.notification.event;

import com.tradev.domain.trade.entity.Trade;
import lombok.Getter;

@Getter
public class TradeAcceptedEvent {
    private final Trade trade;

    public TradeAcceptedEvent(Trade trade) {
        this.trade = trade;
    }
}
