package com.likelion.firstbite.firstbiteserver.common.exception;

import com.likelion.firstbite.firstbiteserver.common.api.ErrorResponse;
import com.likelion.firstbite.firstbiteserver.common.api.ErrorResponse.FieldErrorDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<FieldErrorDetail> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList();
        String code = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getCode() == null ? "" : error.getCode())
                .anyMatch("ValidPassword"::equals) ? "AUTH_PASSWORD_POLICY_VIOLATION"
                : exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getCode() == null ? "" : error.getCode())
                .anyMatch(codeName -> codeName.equals("MinAge") || codeName.equals("Past"))
                ? "AUTH_BIRTH_DATE_INVALID"
                : exception.getBindingResult().getFieldErrors().stream()
                .anyMatch(error -> (error.getField().equals("termsAgreed") || error.getField().equals("privacyAgreed")))
                ? "AUTH_REQUIRED_TERMS_NOT_AGREED"
                : "COMMON_INVALID_REQUEST";
        return ResponseEntity.badRequest().body(ErrorResponse.of(code, "요청 값이 올바르지 않습니다.", details));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(ErrorResponse.of(exception.getCode(), exception.getMessage()));
    }
}
