package com.tradev.domain.trade.service;

import com.tradev.common.dto.CursorPageResponse;
import com.tradev.common.exception.ErrorCode;
import com.tradev.common.exception.TradevException;
import com.tradev.domain.item.entity.Item;
import com.tradev.domain.item.entity.ItemStatus;
import com.tradev.domain.item.repository.ItemRepository;
import com.tradev.domain.notification.event.TradeCancelledEvent;
import com.tradev.domain.notification.event.TradeCompletedEvent;
import com.tradev.domain.notification.event.TradeRejectedEvent;
import com.tradev.domain.notification.event.TradeRequestedEvent;
import com.tradev.domain.notification.event.TradeAcceptedEvent;
import com.tradev.domain.trade.dto.TradeRequest;
import com.tradev.domain.trade.dto.TradeResponse;
import com.tradev.domain.trade.entity.Trade;
import com.tradev.domain.trade.entity.TradeStatus;
import com.tradev.domain.trade.repository.TradeRepository;
import com.tradev.domain.user.entity.User;
import com.tradev.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeService {

    private final TradeRepository tradeRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TradeResponse requestTrade(Long buyerId, TradeRequest request) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new TradevException(ErrorCode.USER_NOT_FOUND));
        Item item = itemRepository.findByIdWithDetails(request.getItemId())
                .orElseThrow(() -> new TradevException(ErrorCode.ITEM_NOT_FOUND));

        if (item.getSeller().getId().equals(buyerId)) {
            throw new TradevException(ErrorCode.USER_CANNOT_SELF_TRADE);
        }
        if (item.getStatus() != ItemStatus.SALE) {
            throw new TradevException(ErrorCode.ITEM_NOT_AVAILABLE);
        }

        boolean hasPending = tradeRepository.existsByItemIdAndBuyerIdAndStatusIn(
                item.getId(), buyerId, List.of(TradeStatus.PENDING, TradeStatus.RESERVED));
        if (hasPending) {
            throw new TradevException(ErrorCode.TRADE_DUPLICATE_REQUEST);
        }

        Trade trade = Trade.builder()
                .item(item)
                .buyer(buyer)
                .seller(item.getSeller())
                .price(item.getPrice())
                .build();

        Trade saved = tradeRepository.save(trade);
        eventPublisher.publishEvent(new TradeRequestedEvent(saved));
        return new TradeResponse(saved);
    }

    public CursorPageResponse<TradeResponse> getTrades(Long userId, String statusStr, String cursor, int pageSize) {
        TradeStatus status = statusStr != null ? TradeStatus.valueOf(statusStr) : null;
        LocalDateTime cursorCreatedAt = null;
        Long cursorId = null;

        if (cursor != null) {
            String[] parts = cursor.split("\\|");
            if (parts.length == 2) {
                cursorCreatedAt = LocalDateTime.parse(parts[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                cursorId = Long.parseLong(parts[1]);
            }
        }

        List<Trade> trades = tradeRepository.findByUserWithCursor(userId, status, cursorCreatedAt, cursorId, pageSize + 1);
        List<TradeResponse> responses = trades.stream().map(TradeResponse::new).toList();
        return CursorPageResponse.of(responses, pageSize,
                r -> r.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "|" + r.getId());
    }

    public TradeResponse getTrade(Long tradeId, Long userId) {
        Trade trade = tradeRepository.findByIdWithDetails(tradeId)
                .orElseThrow(() -> new TradevException(ErrorCode.TRADE_NOT_FOUND));
        if (!trade.isParticipant(userId)) {
            throw new TradevException(ErrorCode.TRADE_NOT_ALLOWED);
        }
        return new TradeResponse(trade);
    }

    @Transactional
    public TradeResponse acceptTrade(Long tradeId, Long userId) {
        Trade trade = tradeRepository.findByIdWithDetails(tradeId)
                .orElseThrow(() -> new TradevException(ErrorCode.TRADE_NOT_FOUND));

        if (!trade.isSeller(userId)) throw new TradevException(ErrorCode.TRADE_NOT_ALLOWED);
        if (trade.getStatus() != TradeStatus.PENDING) throw new TradevException(ErrorCode.TRADE_INVALID_STATUS_TRANSITION);

        trade.accept();
        trade.getItem().setStatus(ItemStatus.RESERVED);
        eventPublisher.publishEvent(new TradeAcceptedEvent(trade));
        return new TradeResponse(trade);
    }

    @Transactional
    public TradeResponse rejectTrade(Long tradeId, Long userId) {
        Trade trade = tradeRepository.findByIdWithDetails(tradeId)
                .orElseThrow(() -> new TradevException(ErrorCode.TRADE_NOT_FOUND));

        if (!trade.isSeller(userId)) throw new TradevException(ErrorCode.TRADE_NOT_ALLOWED);
        if (trade.getStatus() != TradeStatus.PENDING) throw new TradevException(ErrorCode.TRADE_INVALID_STATUS_TRANSITION);

        trade.reject();
        eventPublisher.publishEvent(new TradeRejectedEvent(trade));
        return new TradeResponse(trade);
    }

    @Transactional
    public TradeResponse confirmTrade(Long tradeId, Long userId) {
        Trade trade = tradeRepository.findByIdWithDetails(tradeId)
                .orElseThrow(() -> new TradevException(ErrorCode.TRADE_NOT_FOUND));

        if (!trade.isParticipant(userId)) throw new TradevException(ErrorCode.TRADE_NOT_ALLOWED);
        if (trade.getStatus() != TradeStatus.RESERVED) throw new TradevException(ErrorCode.TRADE_INVALID_STATUS_TRANSITION);

        boolean completed = trade.confirm(trade.isBuyer(userId));
        if (completed) {
            trade.getItem().setStatus(ItemStatus.COMPLETED);
            // 신뢰 점수 +5 (양측)
            trade.getBuyer().addTrustScore(5);
            trade.getSeller().addTrustScore(5);
            eventPublisher.publishEvent(new TradeCompletedEvent(trade));
        }
        return new TradeResponse(trade);
    }

    @Transactional
    public TradeResponse cancelTrade(Long tradeId, Long userId, String reason) {
        Trade trade = tradeRepository.findByIdWithDetails(tradeId)
                .orElseThrow(() -> new TradevException(ErrorCode.TRADE_NOT_FOUND));

        if (!trade.isParticipant(userId)) throw new TradevException(ErrorCode.TRADE_NOT_ALLOWED);
        if (trade.getStatus() == TradeStatus.COMPLETED) throw new TradevException(ErrorCode.TRADE_ALREADY_COMPLETED);
        if (trade.getStatus() == TradeStatus.CANCELLED || trade.getStatus() == TradeStatus.REJECTED) {
            throw new TradevException(ErrorCode.TRADE_INVALID_STATUS_TRANSITION);
        }

        TradeStatus prevStatus = trade.getStatus();
        trade.cancel(reason);
        if (prevStatus == TradeStatus.RESERVED) {
            trade.getItem().setStatus(ItemStatus.SALE);
        }
        eventPublisher.publishEvent(new TradeCancelledEvent(trade));
        return new TradeResponse(trade);
    }
}
