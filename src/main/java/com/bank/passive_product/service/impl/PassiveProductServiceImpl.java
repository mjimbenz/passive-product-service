package com.bank.passive_product.service.impl;

import com.bank.passive_product.client.CustomerWebClient;
import com.bank.passive_product.client.entity.Customer;
import com.bank.passive_product.exception.BusinessException;
import com.bank.passive_product.model.PassiveProductEntity;
import com.bank.passive_product.repository.PassiveProductRepository;
import com.bank.passive_product.service.PassiveProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PassiveProductServiceImpl implements PassiveProductService {

    private final PassiveProductRepository repository;
    private final CustomerWebClient customerWebClient;

    @Override
    public Flux<PassiveProductEntity> findAll() {
        return repository.findByActiveTrue();
    }

    @Override
    public Mono<PassiveProductEntity> findById(String id) {
       return repository.findByIdAndActiveTrue(id)
               .switchIfEmpty(Mono.error(new BusinessException("Product not found or inactive")));
    }

    @Override
    public Mono<PassiveProductEntity> create(PassiveProductEntity e) {

        e.setActive(true);
        e.setCreatedAt(LocalDateTime.now());
        e.setBalance(0.0);

        return validateCustomerExists(e.getCustomerId())
                .flatMap(customer -> {
                    if(customer.type().equals("PERSONAL")) {
                        return validatePersonalCustomer(e.getCustomerId(), e.getAccountType());
                    } else {
                        return validateBusinessCustomer(e.getAccountType());
                    }
                })
                .then(applyAccountRules(e))
                .flatMap(repository::save);
    }

    @Override
    public Mono<PassiveProductEntity> update(String id, PassiveProductEntity e) {

        return repository.findByIdAndActiveTrue(id)
                .switchIfEmpty(Mono.error(new BusinessException("Passive product not found")))
                // 1. Obtener entidad actual
                .flatMap(existing ->
                        validateCustomerExists(existing.getCustomerId())    // 2. Validar cliente
                                .flatMap(customer -> {
                                    // 3. Aplicar reglas según tipo cliente
                                    if(customer.type().equals("PERSONAL")) {
                                        return validatePersonalCustomer(existing.getCustomerId(), e.getAccountType());
                                    } else {
                                        return validateBusinessCustomer(e.getAccountType());
                                    }
                                })
                                // 4. Aplicar reglas del tipo de cuenta
                                .then(applyAccountRules(e))
                                // 5. Actualizar
                                .flatMap(validated -> {
                                    existing.setBalance(validated.getBalance());
                                    existing.setUpdatedAt(LocalDateTime.now());
                                    return repository.save(existing);
                                })
                );
    }


    @Override
    public Mono<Void> delete(String id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException("Product not found")))
                .flatMap(entity -> {
                    entity.setActive(false);
                    entity.setDeletedAt(LocalDateTime.now());
                    return repository.save(entity);
                })
                .then();

    }

    @Override
    public Mono<Double> getBalance(String id) {
        return repository.findById(id)
                .map(PassiveProductEntity::getBalance);
    }


    private Mono<Customer> validateCustomerExists(String customerId) {
        return customerWebClient.getCustomer(customerId)
                .switchIfEmpty(Mono.error(new BusinessException("Customer not found")));
    }


    private Mono<Void> validatePersonalCustomer(String customerId, String accountType) {

        return repository.findByCustomerId(customerId)
                .collectList()
                .flatMap(accounts -> {

                    if(accountType.equals("SAVING") &&
                            accounts.stream().anyMatch(a -> a.getAccountType().equals("SAVING"))) {
                        return Mono.error(new BusinessException("Personal customer already has a saving account"));
                    }

                    if(accountType.equals("CURRENT") &&
                            accounts.stream().anyMatch(a -> a.getAccountType().equals("CURRENT"))) {
                        return Mono.error(new BusinessException("Personal customer already has a current account"));
                    }

                    // FIXED_TERM: allowed multiple – no checks

                    return Mono.empty();
                });
    }


    private Mono<Void> validateBusinessCustomer(String accountType) {

        if(accountType.equals("SAVING") || accountType.equals("FIXED_TERM")) {
            return Mono.error(new BusinessException(
                    "Business customers cannot have saving or fixed-term accounts"
            ));
        }
        return Mono.empty();
    }


    private Mono<PassiveProductEntity> applyAccountRules(PassiveProductEntity e) {

        switch (e.getAccountType()) {

            case "SAVING":
                e.setMaintenanceFee(0.0);
                if(e.getTransactionLimit() == null)
                    e.setTransactionLimit(10); // default
                break;

            case "CURRENT":
                if(e.getMaintenanceFee() == null)
                    e.setMaintenanceFee(25.0); // example
                e.setTransactionLimit(null);
                break;

            case "FIXED_TERM":
                e.setMaintenanceFee(0.0);
                e.setTransactionLimit(1);
                if(e.getAllowedMovementDay() == null)
                    return Mono.error(new BusinessException("Fixed term accounts require allowedMovementDay"));
                break;

            default:
                return Mono.error(new BusinessException("Invalid account type"));
        }
        return Mono.just(e);
    }




}
