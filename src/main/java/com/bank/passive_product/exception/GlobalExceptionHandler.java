package com.bank.passive_product.exception;

import com.bank.passive_product.exception.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private ErrorResponse buildError(String code, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .errorCode(code)
                .message(message)
                .path(path)
                .microservice("passive-product-service")
                .build();
    }

    // -------------------------
    // 1. BusinessException
    // -------------------------
    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBusinessException(
            BusinessException ex, ServerWebExchange exchange) {

        log.warn("Business error: {}", ex.getMessage());

        return Mono.just(
                ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(buildError(
                                "BUSINESS_ERROR",
                                ex.getMessage(),
                                exchange.getRequest().getPath().value()
                        ))
        );
    }

    // -------------------------
    // 2. Errors from external services (WebClient)
    // -------------------------
    @ExceptionHandler(WebClientResponseException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleWebClientError(
            WebClientResponseException ex, ServerWebExchange exchange) {

        log.error("External service error [{}]: {}", ex.getStatusCode(), ex.getMessage());

        return Mono.just(
                ResponseEntity
                        .status(ex.getStatusCode())
                        .body(buildError(
                                "EXTERNAL_SERVICE_ERROR",
                                ex.getResponseBodyAsString(),
                                exchange.getRequest().getPath().value()
                        ))
        );
    }

    // -------------------------
    // 3. Validation errors (@Valid, @NotNull, etc.)
    // -------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(
            MethodArgumentNotValidException ex, ServerWebExchange exchange) {

        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("Validation error: {}", errors);

        return Mono.just(
                ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(buildError(
                                "VALIDATION_ERROR",
                                errors,
                                exchange.getRequest().getPath().value()
                        ))
        );
    }

    // -------------------------
    // 4. ANY UNKNOWN ERROR
    // -------------------------
    @ExceptionHandler(Throwable.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericError(
            Throwable ex, ServerWebExchange exchange) {

        log.error("Unexpected error: {}", ex.getMessage(), ex);

        return Mono.just(
                ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(buildError(
                                "INTERNAL_ERROR",
                                "An unexpected error occurred",
                                exchange.getRequest().getPath().value()
                        ))
        );
    }
}