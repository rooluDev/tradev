package com.tradev.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // COMMON
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "입력값이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_002", "요청한 리소스를 찾을 수 없습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "COMMON_003", "접근 권한이 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_004", "서버 내부 오류가 발생했습니다."),
    OPTIMISTIC_LOCK_CONFLICT(HttpStatus.CONFLICT, "COMMON_005", "동시 요청으로 인한 충돌이 발생했습니다. 다시 시도해주세요."),

    // AUTH
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_001", "이메일 또는 비밀번호가 올바르지 않습니다."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_002", "인증 토큰이 만료되었습니다."),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_003", "유효하지 않은 인증 토큰입니다."),
    AUTH_REFRESH_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_004", "Refresh Token이 유효하지 않습니다."),
    AUTH_EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "AUTH_005", "이메일 인증이 필요합니다."),
    AUTH_EMAIL_ALREADY_VERIFIED(HttpStatus.BAD_REQUEST, "AUTH_006", "이미 인증된 이메일입니다."),
    AUTH_VERIFY_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH_007", "이메일 인증 토큰이 만료되었습니다."),
    AUTH_VERIFY_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "AUTH_008", "유효하지 않은 이메일 인증 토큰입니다."),
    AUTH_RESET_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH_009", "비밀번호 재설정 토큰이 만료되었습니다."),
    AUTH_RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "AUTH_010", "유효하지 않은 비밀번호 재설정 토큰입니다."),
    AUTH_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AUTH_011", "너무 많은 요청입니다. 잠시 후 다시 시도해주세요."),

    // USER
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),
    USER_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "USER_002", "이미 사용 중인 이메일입니다."),
    USER_NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "USER_003", "이미 사용 중인 닉네임입니다."),
    USER_SUSPENDED(HttpStatus.FORBIDDEN, "USER_004", "정지된 계정입니다."),
    USER_WITHDRAWN(HttpStatus.GONE, "USER_005", "탈퇴한 계정입니다."),
    USER_CANNOT_SELF_TRADE(HttpStatus.BAD_REQUEST, "USER_006", "자기 자신과 거래할 수 없습니다."),

    // ITEM
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "ITEM_001", "상품을 찾을 수 없습니다."),
    ITEM_NOT_OWNER(HttpStatus.FORBIDDEN, "ITEM_002", "상품 소유자가 아닙니다."),
    ITEM_NOT_AVAILABLE(HttpStatus.CONFLICT, "ITEM_003", "거래 가능하지 않은 상품입니다."),
    ITEM_IN_PROGRESS(HttpStatus.CONFLICT, "ITEM_004", "거래 진행 중인 상품은 삭제할 수 없습니다."),
    ITEM_BOOST_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "ITEM_005", "오늘은 이미 끌어올리기를 사용했습니다."),

    // TRADE
    TRADE_NOT_FOUND(HttpStatus.NOT_FOUND, "TRADE_001", "거래를 찾을 수 없습니다."),
    TRADE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "TRADE_002", "해당 거래에 대한 권한이 없습니다."),
    TRADE_INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "TRADE_003", "유효하지 않은 거래 상태 전이입니다."),
    TRADE_DUPLICATE_REQUEST(HttpStatus.CONFLICT, "TRADE_004", "이미 진행 중인 거래 요청이 있습니다."),
    TRADE_ALREADY_COMPLETED(HttpStatus.CONFLICT, "TRADE_005", "이미 완료된 거래입니다."),

    // SLOT / RESERVATION
    SLOT_NOT_FOUND(HttpStatus.NOT_FOUND, "SLOT_001", "슬롯을 찾을 수 없습니다."),
    SLOT_NOT_AVAILABLE(HttpStatus.CONFLICT, "SLOT_002", "예약 불가능한 슬롯입니다."),
    SLOT_ALREADY_LOCKED(HttpStatus.CONFLICT, "SLOT_003", "다른 사용자가 예약 중인 슬롯입니다."),
    SLOT_LOCK_EXPIRED(HttpStatus.CONFLICT, "SLOT_004", "슬롯 잠금이 만료되었습니다. 다시 시도해주세요."),
    SLOT_DUPLICATE(HttpStatus.CONFLICT, "SLOT_005", "이미 해당 시간에 슬롯이 존재합니다."),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "SLOT_006", "예약을 찾을 수 없습니다."),
    RESERVATION_CANCEL_TOO_LATE(HttpStatus.BAD_REQUEST, "SLOT_007", "예약 24시간 이전에만 취소 가능합니다."),
    SLOT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "SLOT_008", "예약에 대한 권한이 없습니다."),
    SLOT_INVALID_STATUS(HttpStatus.CONFLICT, "SLOT_009", "유효하지 않은 예약 상태입니다."),
    SLOT_INVALID_TIME(HttpStatus.BAD_REQUEST, "SLOT_010", "유효하지 않은 슬롯 시간입니다."),
    SLOT_ALREADY_RESERVED(HttpStatus.CONFLICT, "SLOT_011", "이미 해당 슬롯에 예약 요청이 있습니다."),
    SLOT_SELF_RESERVATION(HttpStatus.BAD_REQUEST, "SLOT_012", "자신의 슬롯에 예약할 수 없습니다."),
    SLOT_LOCKED(HttpStatus.CONFLICT, "SLOT_013", "다른 사용자가 예약 중인 슬롯입니다."),

    // CHAT
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_001", "채팅방을 찾을 수 없습니다."),
    CHAT_NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "CHAT_002", "채팅방 참여자가 아닙니다."),

    // NOTIFICATION
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTI_001", "알림을 찾을 수 없습니다."),
    NOTIFICATION_NOT_OWNER(HttpStatus.FORBIDDEN, "NOTI_002", "알림 소유자가 아닙니다."),

    // REVIEW
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW_001", "리뷰를 찾을 수 없습니다."),
    REVIEW_NOT_ALLOWED(HttpStatus.FORBIDDEN, "REVIEW_002", "리뷰 작성 권한이 없습니다."),
    REVIEW_DUPLICATE(HttpStatus.CONFLICT, "REVIEW_003", "이미 리뷰를 작성했습니다."),
    REVIEW_PERIOD_EXPIRED(HttpStatus.BAD_REQUEST, "REVIEW_004", "리뷰 작성 기간(7일)이 지났습니다."),
    REVIEW_TRADE_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "REVIEW_005", "완료된 거래만 리뷰 작성이 가능합니다."),
    REVIEW_REPLY_DUPLICATE(HttpStatus.CONFLICT, "REVIEW_006", "이미 답글을 작성했습니다."),

    // REPORT
    REPORT_DUPLICATE(HttpStatus.CONFLICT, "REPORT_001", "이미 신고한 대상입니다."),
    REPORT_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "REPORT_002", "자기 자신을 신고할 수 없습니다."),

    // AI
    AI_GENERATION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "AI_001", "AI 응답 생성에 실패했습니다."),
    AI_DAILY_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AI_002", "AI 기능 일일 사용 한도를 초과했습니다."),

    // CATEGORY
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CAT_001", "카테고리를 찾을 수 없습니다."),

    // FILE
    FILE_INVALID_TYPE(HttpStatus.BAD_REQUEST, "FILE_001", "지원하지 않는 파일 형식입니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "FILE_002", "파일 크기가 초과되었습니다. (최대 5MB)"),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_003", "파일 업로드에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
