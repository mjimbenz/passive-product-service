package com.bank.passive_product.config;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class LoggingContextWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        // -------------------------
        // 1. Obtain distributed tracing context
        // -------------------------
        String traceId = exchange.getRequest().getHeaders().getFirst("X-B3-TraceId");
        String spanId = exchange.getRequest().getHeaders().getFirst("X-B3-SpanId");

        // If gateway did not send tracing headers → generate them
        if (traceId == null) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        if (spanId == null) {
            spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        // -------------------------
        // 2. Extract business context
        // -------------------------
        String customerId = exchange.getRequest().getQueryParams().getFirst("customerId");
        String productId = exchange.getRequest().getQueryParams().getFirst("id");

        // If path includes /customer/{customerId}, capture that value
        String path = exchange.getRequest().getPath().value();
        if (path.contains("/customer/")) {
            String[] parts = path.split("/");
            int idx = -1;
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("customer")) {
                    idx = i;
                    break;
                }
            }
            if (idx != -1 && parts.length > idx + 1) {
                customerId = parts[idx + 1];
            }
        }

        // -------------------------
        // 3. Put everything into MDC
        // -------------------------
        MDC.put("traceId", traceId);
        MDC.put("spanId", spanId);
        MDC.put("customerId", customerId);
        MDC.put("productId", productId);

        log.debug("[MDC] traceId={}, spanId={}, customerId={}, productId={}",
                traceId, spanId, customerId, productId);

        // -------------------------
        // 4. Execute request and CLEAR MDC AT THE END
        // -------------------------
        return chain.filter(exchange)
                .doFinally(signalType -> MDC.clear()
                );
    }


}

