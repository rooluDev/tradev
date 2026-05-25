package com.tradev.domain.notification.entity;

public enum NotificationType {
    TRADE_REQUESTED,   // 거래 요청 받음 (판매자)
    TRADE_ACCEPTED,    // 거래 수락됨 (구매자)
    TRADE_REJECTED,    // 거래 거절됨 (구매자)
    TRADE_CANCELLED,   // 거래 취소됨
    TRADE_COMPLETED,   // 거래 완료됨
    RESERVATION_REQUESTED,  // 예약 요청 받음 (판매자)
    RESERVATION_ACCEPTED,   // 예약 수락됨 (구매자)
    RESERVATION_CANCELLED,  // 예약 취소됨
    RESERVATION_REMINDER,   // 예약 24시간 전 리마인더
    CHAT_MESSAGE,           // 새 채팅 메시지
    REVIEW_RECEIVED,        // 리뷰 받음
    REPORT_PROCESSED        // 신고 처리됨 (관리자)
}
