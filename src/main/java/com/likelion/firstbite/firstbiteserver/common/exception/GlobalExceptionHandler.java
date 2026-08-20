package com.likelion.firstbite.firstbiteserver.common.exception;

import com.likelion.firstbite.firstbiteserver.common.api.ErrorResponse;
import com.likelion.firstbite.firstbiteserver.common.api.ErrorResponse.FieldErrorDetail;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    // 업로드 파일 크기 초과: 멀티파트 리졸버가 컨트롤러 진입 전에 던지므로 여기서 규격화한다.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUpload(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ErrorResponse.of("IMAGE_TOO_LARGE", "이미지는 10MB 이하여야 합니다."));
    }

    // 필수 요청 파트/헤더/파라미터 누락, 타입 불일치, 파싱 불가 → 일관된 400
    @ExceptionHandler({
            MissingServletRequestPartException.class,
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception exception) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("COMMON_INVALID_REQUEST", "요청 값이 올바르지 않습니다."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of("COMMON_METHOD_NOT_ALLOWED", "지원하지 않는 요청 메서드입니다."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorResponse.of("COMMON_UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 미디어 타입입니다."));
    }

    // 최종 fallback: 처리되지 않은 모든 예외를 규격화된 500으로. 원인은 서버 로그로만 남긴다(클라이언트 미노출).
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("처리되지 않은 예외", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("COMMON_INTERNAL_ERROR", "서버 오류가 발생했습니다."));
    }
}
