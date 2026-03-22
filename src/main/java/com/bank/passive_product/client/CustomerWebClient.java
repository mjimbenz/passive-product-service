package com.bank.passive_product.client;


import com.bank.passive_product.client.entity.Customer;
import com.bank.passive_product.exception.BusinessException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerWebClient {

    private final WebClient client = WebClient.builder()
            .baseUrl("http://localhost:8080/customer-service")
            .build();

    @CircuitBreaker(name = "customerService", fallbackMethod = "getCustomerFallback")
    @Retry(name = "customerService")
    @TimeLimiter(name = "customerService")
    public Mono<Customer> getCustomer(String id) {
        return client.get().uri("/{id}", id)
                .retrieve()
                .bodyToMono(Customer.class);
    }


    private Mono<Customer> fallbackCustomer(String id, Throwable t) {
        return Mono.error(new BusinessException("Customer Service is unavailable"));
    }

}
