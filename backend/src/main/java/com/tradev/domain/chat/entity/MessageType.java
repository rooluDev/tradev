package com.tradev.domain.chat.entity;

public enum MessageType {
    TEXT,         // 일반 텍스트
    PRICE_OFFER,  // 가격 제안
    IMAGE,        // 이미지
    SYSTEM        // 시스템 메시지 (거래 상태 변경 등)
}
