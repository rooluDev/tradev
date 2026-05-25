package com.tradev.domain.reservation.entity;

public enum ReservationStatus {
    PENDING,    // 예약 요청
    CONFIRMED,  // 수락됨
    COMPLETED,  // 완료
    CANCELLED   // 취소
}
