package com.tradev.domain.notification.service;

import com.tradev.domain.notification.entity.NotificationType;
import com.tradev.domain.notification.event.ChatMessageEvent;
import com.tradev.domain.notification.event.TradeAcceptedEvent;
import com.tradev.domain.notification.event.TradeCancelledEvent;
import com.tradev.domain.notification.event.TradeCompletedEvent;
import com.tradev.domain.notification.event.TradeRejectedEvent;
import com.tradev.domain.notification.event.TradeRequestedEvent;
import com.tradev.domain.trade.entity.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMessage(ChatMessageEvent event) {
        notificationService.createAndSend(
            event.getRecipientId(),
            NotificationType.CHAT_MESSAGE,
            String.format("%s님이 메시지를 보냈습니다: %s", event.getSenderNickname(),
                event.getContentPreview().length() > 20
                    ? event.getContentPreview().substring(0, 20) + "..."
                    : event.getContentPreview()),
            "/chat/" + event.getRoomId()
        );
        log.debug("[Notification] CHAT_MESSAGE → recipientId={}", event.getRecipientId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
