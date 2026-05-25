package com.tradev.domain.notification.event;

import com.tradev.domain.trade.entity.Trade;
import lombok.Getter;

@Getter
public class TradeCancelledEvent {
    private final Trade trade;

    public TradeCancelledEvent(Trade trade) {
        this.trade = trade;
    }
}
