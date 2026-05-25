package com.tradev.common.exception;

import lombok.Getter;

@Getter
public class TradevException extends RuntimeException {

    private final ErrorCode errorCode;

    public TradevException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public TradevException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
