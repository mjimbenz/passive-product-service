package com.bank.passive_product;


import com.bank.passive_product.config.LoggingContextWebFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class LoggingContextWebFilterTest {

    private LoggingContextWebFilter filter;
    private WebFilterChain chain;

    @BeforeEach
    void setup() {
        filter = new LoggingContextWebFilter();
        chain = Mockito.mock(WebFilterChain.class);

        Mockito.when(chain.filter(Mockito.any()))
                .thenReturn(Mono.empty());
    }

    // ------------------------------------------------------------------------
    // 1. When no headers are present → generate IDs
    // ------------------------------------------------------------------------
    @Test
    void filter_shouldGenerateTraceIdAndSpanId_whenMissingHeaders() {

        MockServerHttpRequest request = MockServerHttpRequest.get("/products").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assert MDC.get("traceId") == null;
        assert MDC.get("spanId") == null;
        assert MDC.get("customerId") == null;
        assert MDC.get("productId") == null;

        Mockito.verify(chain).filter(Mockito.any());
    }

    // ------------------------------------------------------------------------
    // 2. Use headers for traceId and spanId
    // ------------------------------------------------------------------------
    @Test
    void filter_shouldUseHeadersIfPresent() {

        MockServerHttpRequest request = MockServerHttpRequest.get("/products")
                .header("X-B3-TraceId", "ABC123")
                .header("X-B3-SpanId", "SPAN123")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assert MDC.get("traceId") == null;
        assert MDC.get("spanId") == null;

        Mockito.verify(chain).filter(Mockito.any());
    }

    // ------------------------------------------------------------------------
    // 3. Extract customerId and productId from query params
    // ------------------------------------------------------------------------
    @Test
    void filter_shouldExtractIdsFromQueryParams() {

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/products?customerId=C1&id=P55")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assert MDC.get("customerId") == null;
        assert MDC.get("productId") == null;

        Mockito.verify(chain).filter(Mockito.any());
    }

    // ------------------------------------------------------------------------
    // 4. Extract customerId from path /customer/{id}
    // ------------------------------------------------------------------------
    @Test
    void filter_shouldExtractCustomerIdFromPath() {

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/v1/customer/CUST777/products")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assert MDC.get("customerId") == null;

        Mockito.verify(chain).filter(Mockito.any());
    }

    // ------------------------------------------------------------------------
    // 5. Ensure MDC clears after completion
    // ------------------------------------------------------------------------
    @Test
    void filter_shouldClearMdcAfterProcessing() {

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header("X-B3-TraceId", "TRACE999")
                .header("X-B3-SpanId", "SPAN999")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assert MDC.get("traceId") == null;
        assert MDC.get("spanId") == null;

        Mockito.verify(chain).filter(Mockito.any());
    }
}