package com.bank.passive_product.exception;

import com.bank.passive_product.exception.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // -----------------------------------------------------
    // 1. BUSINESS EXCEPTION (errores de reglas de negocio)
    // -----------------------------------------------------
    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBusinessException(
            BusinessException ex, ServerWebExchange exchange) {

        log.warn("[BusinessException] {} | path={}", ex.getMessage(), exchange.getRequest().getPath());

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

    // -----------------------------------------------------
    // 2. DECODING EXCEPTION (JSON mal formado o ENUM inválido)
    // -----------------------------------------------------
    @ExceptionHandler(org.springframework.core.codec.DecodingException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDecodingException(
            Exception ex, ServerWebExchange exchange) {

        log.error("[DecodingException] Error leyendo el body. Causa: {} | path={}",
                ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage(),
                exchange.getRequest().getPath());

        return Mono.just(
                ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(buildError(
                                "INVALID_REQUEST",
                                "El formato del cuerpo enviado es inválido. Verifica tipos, nombre de campos y ENUMs.",
                                exchange.getRequest().getPath().value()
                        ))
        );
    }

    // -----------------------------------------------------
    // 3. VALIDATION EXCEPTION
    // -----------------------------------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(
            MethodArgumentNotValidException ex, ServerWebExchange exchange) {

        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(field -> field.getField() + " " + field.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("[ValidationException] {} | path={}", errors, exchange.getRequest().getPath());

        return Mono.just(
                ResponseEntity.badRequest()
                        .body(buildError(
                                "VALIDATION_ERROR",
                                errors,
                                exchange.getRequest().getPath().value()
                        ))
        );
    }

    // -----------------------------------------------------
    // 4. EXTERNAL SERVICE RESPONSE EXCEPTION (WebClient errores HTTP)
    // -----------------------------------------------------
    @ExceptionHandler(WebClientResponseException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleWebClientError(
            WebClientResponseException ex, ServerWebExchange exchange) {

        log.error("[ExternalServiceError] status={} body='{}' path={}",
                ex.getStatusCode(),
                ex.getResponseBodyAsString(),
                exchange.getRequest().getPath());

        return Mono.just(
                ResponseEntity
                        .status(ex.getStatusCode())
                        .body(buildError(
                                "EXTERNAL_SERVICE_ERROR",
                                ex.getStatusText(),
                                exchange.getRequest().getPath().value()
                        ))
        );
    }

    // -----------------------------------------------------
    // 5. CAJA NEGRA: Cualquier error desconocido
    // -----------------------------------------------------
    @ExceptionHandler(Throwable.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericError(
            Throwable ex, ServerWebExchange exchange) {

        log.error("[UnexpectedError] {} | cause={} | path={}",
                ex.getMessage(),
                ex.getCause() != null ? ex.getCause().getMessage() : "none",
                exchange.getRequest().getPath(),
                ex // stacktrace
        );

        return Mono.just(
                ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(buildError(
                                "INTERNAL_ERROR",
                                "Ocurrió un error inesperado en el servicio.",
                                exchange.getRequest().getPath().value()
                        ))
        );
    }
}