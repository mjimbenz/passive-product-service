package com.bank.passive_product.service.impl;

import com.bank.passive_product.model.PassiveProductEntity;
import com.bank.passive_product.repository.PassiveProductRepository;
import com.bank.passive_product.service.PassiveProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
@Service
@RequiredArgsConstructor
public class PassiveProductServiceImpl implements PassiveProductService {

    private final PassiveProductRepository repository;
    @Override
    public Flux<PassiveProductEntity> findAll() {
        return repository.findAll();
    }

    @Override
    public Mono<PassiveProductEntity> findById(String id) {
       return repository.findById(id);
    }

    @Override
    public Mono<PassiveProductEntity> create(PassiveProductEntity e) {
        return repository.save(e);
    }

    @Override
    public Mono<PassiveProductEntity> update(String id, PassiveProductEntity e) {
        return repository.findById(id)
                .flatMap(existing -> {
                    existing.setCustomerId(e.getCustomerId());
                    existing.setAccountType(e.getAccountType());
                    existing.setBalance(e.getBalance());
                    existing.setTransactionLimit(e.getTransactionLimit());
                    existing.setMaintenanceFee(e.getMaintenanceFee());
                    existing.setAllowedMovementDay(e.getAllowedMovementDay());
                    return repository.save(existing);
                });
    }

    @Override
    public Mono<Void> delete(String id) {
        return repository.deleteById(id);
    }

    @Override
    public Mono<Double> getBalance(String id) {
        return repository.findById(id)
                .map(PassiveProductEntity::getBalance);
    }
}
