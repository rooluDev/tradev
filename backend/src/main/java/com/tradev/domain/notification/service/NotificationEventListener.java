package com.tradev.domain.notification.service;

import com.tradev.domain.notification.entity.NotificationType;
import com.tradev.domain.notification.event.TradeAcceptedEvent;
import com.tradev.domain.notification.event.TradeCancelledEvent;
import com.tradev.domain.notification.event.TradeCompletedEvent;
import com.tradev.domain.notification.event.TradeRejectedEvent;
import com.tradev.domain.notification.event.TradeRequestedEvent;
import com.tradev.domain.trade.entity.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @EventListener
    public void onTradeRequested(TradeRequestedEvent event) {
        Trade trade = event.getTrade();
        notificationService.createAndSend(
            trade.getSeller().getId(),
            NotificationType.TRADE_REQUESTED,
            String.format("'%s' 상품에 거래 요청이 왔습니다.", trade.getItem().getTitle()),
            "/trades/" + trade.getId()
        );
        log.debug("[Notification] TRADE_REQUESTED → sellerId={}", trade.getSeller().getId());
    }

    @Async
    @EventListener
    public void onTradeAccepted(TradeAcceptedEvent event) {
        Trade trade = event.getTrade();
        notificationService.createAndSend(
            trade.getBuyer().getId(),
            NotificationType.TRADE_ACCEPTED,
            String.format("'%s' 거래 요청이 수락되었습니다.", trade.getItem().getTitle()),
            "/trades/" + trade.getId()
        );
    }

    @Async
    @EventListener
    public void onTradeRejected(TradeRejectedEvent event) {
        Trade trade = event.getTrade();
        notificationService.createAndSend(
            trade.getBuyer().getId(),
            NotificationType.TRADE_REJECTED,
            String.format("'%s' 거래 요청이 거절되었습니다.", trade.getItem().getTitle()),
            "/trades/" + trade.getId()
        );
    }

    @Async
    @EventListener
    public void onTradeCancelled(TradeCancelledEvent event) {
        Trade trade = event.getTrade();
        // 상대방에게 알림 (취소한 사람이 아닌 쪽)
        // 구매자가 취소 → 판매자에게, 판매자가 취소 → 구매자에게 알림 (서비스에서 구분 가능하나 단순화)
        notificationService.createAndSend(
            trade.getSeller().getId(),
            NotificationType.TRADE_CANCELLED,
            String.format("'%s' 거래가 취소되었습니다.", trade.getItem().getTitle()),
            "/trades/" + trade.getId()
        );
    }

    @Async
    @EventListener
    public void onTradeCompleted(TradeCompletedEvent event) {
        Trade trade = event.getTrade();
        notificationService.createAndSend(
            trade.getBuyer().getId(),
            NotificationType.TRADE_COMPLETED,
            String.format("'%s' 거래가 완료되었습니다. 리뷰를 남겨보세요!", trade.getItem().getTitle()),
            "/trades/" + trade.getId()
        );
        notificationService.createAndSend(
            trade.getSeller().getId(),
            NotificationType.TRADE_COMPLETED,
            String.format("'%s' 거래가 완료되었습니다. 리뷰를 남겨보세요!", trade.getItem().getTitle()),
            "/trades/" + trade.getId()
        );
    }
}
