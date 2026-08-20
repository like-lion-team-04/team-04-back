package com.likelion.firstbite.firstbiteserver.common.exception;

import com.likelion.firstbite.firstbiteserver.common.api.ErrorResponse;
import com.likelion.firstbite.firstbiteserver.common.api.ErrorResponse.FieldErrorDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import tools.jackson.databind.exc.MismatchedInputException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    /**
     * 본문 역직렬화 자체가 실패한 경우(예: birthDate에 "19990101"처럼 ISO 형식이 아닌 값).
     * 기본 처리로는 application/problem+json이 응답되어 클라이언트가 표준 에러 포맷을 읽을 수 없으므로
     * 검증 실패와 동일한 형태로 변환해 어느 필드가 문제인지 전달한다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException exception) {
        List<FieldErrorDetail> details = failedField(exception)
                .map(field -> List.of(new FieldErrorDetail(field, "형식이 올바르지 않습니다.")))
                .orElseGet(List::of);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("COMMON_INVALID_REQUEST", "요청 값이 올바르지 않습니다.", details));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(ErrorResponse.of(exception.getCode(), exception.getMessage()));
    }

    /** 역직렬화가 멈춘 지점의 필드명을 찾는다. 중첩 구조에서는 가장 안쪽 필드를 사용한다. */
    private Optional<String> failedField(HttpMessageNotReadableException exception) {
        if (exception.getCause() instanceof MismatchedInputException mismatch) {
            return mismatch.getPath().stream()
                    .map(reference -> reference.getPropertyName())
                    .filter(Objects::nonNull)
                    .reduce((outer, inner) -> inner);
        }
        return Optional.empty();
    }
}
