package com.bank.passive_product.repository;

import com.bank.passive_product.model.PassiveProductEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface PassiveProductRepository extends ReactiveMongoRepository<PassiveProductEntity, String> {
}
