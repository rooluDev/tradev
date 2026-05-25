package com.tradev.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ValidationError {

    private final boolean success = false;
    private final String code;
    private final String message;
    private final List<FieldError> errors;

    @Getter
    @AllArgsConstructor
    public static class FieldError {
        private final String field;
        private final Object value;
        private final String reason;
    }
}
