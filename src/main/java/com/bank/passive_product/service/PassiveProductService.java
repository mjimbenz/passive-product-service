package com.bank.passive_product.service;

import com.bank.passive_product.model.PassiveProductEntity;
import jakarta.annotation.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PassiveProductService {

    public Flux<PassiveProductEntity> findAll();

    public Mono<PassiveProductEntity> findById(String id);

    public Mono<PassiveProductEntity> create(PassiveProductEntity e);

    public Mono<PassiveProductEntity> update(String id, PassiveProductEntity e);

    public Mono<Void> delete(String id);

    public Mono<Double> getBalance(String id);

    Flux<PassiveProductEntity> findByCustomerId(String customerId, @Nullable String productId);
}

