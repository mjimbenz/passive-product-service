package com.bank.passive_product.client;


import com.bank.passive_product.client.entity.Customer;
import com.bank.passive_product.exception.BusinessException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


@Component
@Slf4j
public class CustomerWebClient {

    private final WebClient client;

    public CustomerWebClient() {
        this.client = WebClient.builder()
                .baseUrl("http://localhost:8080/customer-service")
                .build();
    }


    public CustomerWebClient(WebClient client) {
        this.client = client;
    }

    @CircuitBreaker(name = "customerService", fallbackMethod = "fallbackCustomer")
    @Retry(name = "customerService")
    @TimeLimiter(name = "customerService")
    public Mono<Customer> getCustomer(String id) {
        return client.get().uri("/{id}", id)
                .retrieve()
                .bodyToMono(Customer.class)
                .switchIfEmpty(Mono.error(new BusinessException("Customer not found with id: " + id)));
    }

    public Mono<Customer> fallbackCustomer(String id, Throwable t) {

        log.error("[CustomerWebClient] Fallback triggered for id={} due to: {}", id,
                t != null ? t.getMessage() : "Unknown error");

        if (t instanceof java.util.concurrent.TimeoutException) {
            return Mono.error(new BusinessException("Customer service timeout while fetching id=" + id));
        }

        if (t instanceof org.springframework.web.reactive.function.client.WebClientRequestException) {
            return Mono.error(new BusinessException("Customer service unreachable for id=" + id));
        }

        if (t instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
            return Mono.error(new BusinessException("Customer service returned error for id=" + id));
        }

        // fallback genérico para cualquier error no categorizado
        return Mono.error(new BusinessException("Customer Service is unavailable for id=" + id));

    }

}
