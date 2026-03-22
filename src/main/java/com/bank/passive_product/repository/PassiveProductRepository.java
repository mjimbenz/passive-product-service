package com.bank.passive_product.repository;

import com.bank.passive_product.model.PassiveProductEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PassiveProductRepository extends ReactiveMongoRepository<PassiveProductEntity, String> {
    Flux<PassiveProductEntity> findByCustomerId(String customerId);
    Flux<PassiveProductEntity> findByActiveTrue();

    Mono<PassiveProductEntity> findByIdAndActiveTrue(String id);
}
