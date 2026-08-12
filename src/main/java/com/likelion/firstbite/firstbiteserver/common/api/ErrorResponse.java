package com.likelion.firstbite.firstbiteserver.common.api;

import java.util.List;

public record ErrorResponse(boolean success, ErrorDetail error) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(false, new ErrorDetail(code, message, List.of()));
    }

    public static ErrorResponse of(String code, String message, List<FieldErrorDetail> details) {
        return new ErrorResponse(false, new ErrorDetail(code, message, details));
    }

    public record ErrorDetail(String code, String message, List<FieldErrorDetail> details) {
    }

    public record FieldErrorDetail(String field, String message) {
    }
}
