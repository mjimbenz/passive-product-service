package com.bank.passive_product;

import com.bank.passive_product.exception.GlobalExceptionHandler;
import com.bank.passive_product.exception.BusinessException;
import com.bank.passive_product.exception.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setup() {
        handler = new GlobalExceptionHandler();
    }

    // ------------------------------------------------------------------------
    // BUSINESS EXCEPTION
    // ------------------------------------------------------------------------
    @Test
    void handleBusinessException_shouldReturnBadRequest() {

        BusinessException ex = new BusinessException("Invalid balance");
        ServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/products/123"));

        StepVerifier.create(handler.handleBusinessException(ex, exchange))
                .assertNext(resp -> {
                    assert resp.getStatusCode() == HttpStatus.BAD_REQUEST;

                    ErrorResponse body = resp.getBody();
                    assert body.getErrorCode().equals("BUSINESS_ERROR");
                    assert body.getMessage().equals("Invalid balance");
                    assert body.getPath().equals("/products/123");
                    assert body.getMicroservice().equals("passive-product-service");
                })
                .verifyComplete();
    }



    // ------------------------------------------------------------------------
    // VALIDATION EXCEPTION (@Valid, @NotNull, etc.)
    // ------------------------------------------------------------------------
    @Test
    void handleValidationException_shouldReturnValidationError() {

        // Simulate @Valid error
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "balance", "must not be null"));
        bindingResult.addError(new FieldError("request", "customerId", "must not be empty"));

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(null, bindingResult);

        ServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/validate/data"));

        StepVerifier.create(handler.handleValidationException(ex, exchange))
                .assertNext(resp -> {
                    assert resp.getStatusCode() == HttpStatus.BAD_REQUEST;

                    ErrorResponse body = resp.getBody();
                    assert body.getErrorCode().equals("VALIDATION_ERROR");
                    assert body.getMessage().contains("must not be null");
                    assert body.getMessage().contains("must not be empty");
                    assert body.getPath().equals("/validate/data");
                })
                .verifyComplete();
    }


}
