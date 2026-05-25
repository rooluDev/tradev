package com.tradev.domain.trade.entity;

public enum TradeStatus {
    PENDING,    // 구매 요청
    RESERVED,   // 수락됨 (거래 확정)
    COMPLETED,  // 완료
    CANCELLED,  // 취소
    REJECTED    // 거절
}
