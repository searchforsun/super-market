package com.supermarket.common.web.handler;

import com.supermarket.common.core.exception.BizException;
import com.supermarket.common.core.result.ErrorType;
import com.supermarket.common.core.result.IResultCode;
import com.supermarket.common.core.result.R;
import com.supermarket.common.core.result.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBizException(BizException e) {
        IResultCode rc = e.getResultCode();
        ErrorType errorType = rc.getErrorType();
        HttpStatus status = mapToHttpStatus(errorType);

        if (errorType == ErrorType.SYSTEM_ERROR) {
            log.error("System exception: code={}, message={}", rc.getCode(), e.getMessage(), e);
        } else {
            log.warn("Business exception: code={}, message={}", rc.getCode(), e.getMessage());
        }

        R<Void> body = R.fail(rc);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        log.warn("Validation error: {}", message);
        return ResponseEntity.badRequest().body(R.fail(ResultCode.PARAM_ERROR, message));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Void>> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        log.warn("Bind error: {}", message);
        return ResponseEntity.badRequest().body(R.fail(ResultCode.PARAM_ERROR, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("Constraint violation: {}", message);
        return ResponseEntity.badRequest().body(R.fail(ResultCode.PARAM_ERROR, message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<R<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = String.format("参数'%s'类型不匹配，期望类型'%s'",
                e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown");
        log.warn("Type mismatch: {}", message);
        return ResponseEntity.badRequest().body(R.fail(ResultCode.PARAM_ERROR, message));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        String message = String.format("请求方法'%s'不支持，支持的方法有'%s'",
                e.getMethod(), String.join(", ", e.getSupportedMethods() != null ? e.getSupportedMethods() : new String[0]));
        log.warn("Method not supported: {}", message);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(R.fail(ResultCode.METHOD_NOT_ALLOWED, message));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<R<Void>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("Max upload size exceeded: {}", e.getMessage());
        return ResponseEntity.badRequest().body(R.fail(ResultCode.FILE_SIZE_EXCEEDED));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.internalServerError().body(R.fail(ResultCode.SYSTEM_ERROR));
    }

    private HttpStatus mapToHttpStatus(ErrorType errorType) {
        return switch (errorType) {
            case SUCCESS -> HttpStatus.OK;
            case CLIENT_ERROR -> HttpStatus.BAD_REQUEST;
            case AUTH_ERROR -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN_ERROR -> HttpStatus.FORBIDDEN;
            case NOT_FOUND_ERROR -> HttpStatus.NOT_FOUND;
            case BUSINESS_ERROR -> HttpStatus.CONFLICT;
            case SYSTEM_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
