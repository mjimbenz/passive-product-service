package com.bank.passive_product.service.impl;

import com.bank.passive_product.client.CustomerWebClient;
import com.bank.passive_product.client.entity.Customer;
import com.bank.passive_product.config.LoggingContextWebFilter;
import com.bank.passive_product.exception.BusinessException;
import com.bank.passive_product.model.PassiveProductEntity;
import com.bank.passive_product.repository.PassiveProductRepository;
import com.bank.passive_product.service.PassiveProductService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PassiveProductServiceImpl implements PassiveProductService {

    private final PassiveProductRepository repository;
    private final CustomerWebClient customerWebClient;
    private static final String ACCONT_CURRENT = "CURRENT";
    private static final String ACCONT_SAVING = "SAVING";
    private static final String ACCONT_FIXED_TERM = "FIXED_TERM";

    /*
    * FIND ALL (Only active products) + LOGS
    */
    @Override
    public Flux<PassiveProductEntity> findAll() {
        log.info("[Service] Listing all active passive products - traceId={}");
        return repository.findByActiveTrue()
                .doOnComplete(() -> log.info("[Service] Completed fetching all active products"))
                .doOnError(err -> log.error("[Service] Error fetching all products: {}", err.getMessage()));

    }

    /*
    *   FIND BY ID (Only if active) + LOGS
    */
    @Override
    public Mono<PassiveProductEntity> findById(String id) {
        log.info("[Service] Fetching passive product by id={} - traceId={}", id);
       return repository.findByIdAndActiveTrue(id)
               .switchIfEmpty(Mono.error(new BusinessException("Product not found or inactive")))
               .doOnSuccess(e -> log.info("[Service] Found active product id={}", id))
               .doOnError(err -> log.error("[Service] Error fetching product id={}, err={}", id, err.getMessage()));

    }

    /*
     *   CREATE + VALIDATIONS + LOGS
     */
    @Override
    public Mono<PassiveProductEntity> create(PassiveProductEntity e) {
        log.info("[Service] Creating product -> customerId={}, accountType={}", e.getCustomerId(), e.getAccountType());
        e.setActive(true);
        e.setTransactionLimit(10);
        e.setCreatedAt(LocalDateTime.now());
        e.setBalance(0.0);

        return validateCustomerExists(e.getCustomerId())
                .doOnSuccess(c -> log.info("[Service] Customer validated for create, id={}", e.getCustomerId()))
                .doOnError(err -> log.error("[Service] Customer validation failed for create, error={}", err.getMessage()))
                .flatMap(customer -> {
                    log.info("[Service] Applying customer rules (type={})", customer.type());
                    if(customer.type().equals("PERSONAL")) {
                        return validatePersonalCustomer(e.getCustomerId(), e.getAccountType());
                    } else {
                        return validateBusinessCustomer(e.getAccountType());
                    }
                })
                .then(applyAccountRules(e)
                        .doOnSuccess(p -> log.info("[Service] Account rules applied for type={}", p.getAccountType()))
                )
                .flatMap(repository::save)
                .doOnSuccess(saved -> log.info("[Service] Product created successfully id={}", saved.getId()))
                .doOnError(err -> log.error("[Service] Error creating product: {}", err.getMessage()));

    }

    /*
     *   UPDATE PRODUCT + BUSNISES RULES + Logs
     */
    @Override
    public Mono<PassiveProductEntity> update(String id, PassiveProductEntity e) {
        log.info("[Service] Updating product id={} with accountType={}", id, e.getAccountType());
        return repository.findByIdAndActiveTrue(id)
                .switchIfEmpty(Mono.error(new BusinessException("Passive product not found")))
                .doOnSuccess(ex -> log.info("[Service] Product found for update id={}", id))
                // 1. Obtener entidad actual
                .flatMap(existing ->
                        validateCustomerExists(existing.getCustomerId())
                                .doOnSuccess(c -> log.info("[Service] Customer validated for update, id={}", existing.getCustomerId()))// 2. Validar cliente
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
                                    log.info("[Service] Rules applied for update product id={}", id);
                                    existing.setBalance(validated.getBalance());
                                    existing.setUpdatedAt(LocalDateTime.now());
                                    return repository.save(existing);
                                })
                )
                .doOnSuccess(p -> log.info("[Service] Product updated successfully id={}", id))
                .doOnError(err -> log.error("[Service] Error updating product id={}, err={}", id, err.getMessage()));

    }


    /*
     *   SOFT DELETE PRODUCT + Logs
     */
    @Override
    public Mono<Void> delete(String id) {
        log.info("[Service] Soft deleting product id={}", id);
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException("Product not found")))
                .doOnSuccess(p -> log.info("[Service] Product found for delete id={}", id))
                .flatMap(entity -> {
                    entity.setActive(false);
                    entity.setDeletedAt(LocalDateTime.now());
                    return repository.save(entity);
                })
                .then()
                .doOnSuccess(v -> log.info("[Service] Product soft-deleted id={}", id))
                .doOnError(err -> log.error("[Service] Error deleting product id={}, err={}", id, err.getMessage()));
    }

    /*
     *   GET BALANCE PRODUCT + Logs
     */
    @Override
    public Mono<Double> getBalance(String id) {
        log.info("[Service] Getting balance for id={}", id);
        return repository.findById(id)
                .map(PassiveProductEntity::getBalance)
                .doOnSuccess(bal -> log.info("[Service] Balance for id={} is {}", id, bal))
                .doOnError(err -> log.error("[Service] Error getting balance id={}, err={}", id, err.getMessage()));

    }

    @Override
    public Mono<Double> updateBalance(String id, double amount) {
        log.info("[Service] Updating balance for id={} with amount={}", id, amount);
        return repository.findByIdAndActiveTrue(id)
                .switchIfEmpty(Mono.error(new BusinessException("Product not found or inactive")))
                .flatMap(entity -> {
                    double newBalance = entity.getBalance() + amount;
                    if (newBalance < 0) {
                        return Mono.error(new BusinessException("Insufficient balance"));
                    }
                    entity.setBalance(newBalance);
                    entity.setUpdatedAt(LocalDateTime.now());
                    return repository.save(entity);
                })
                .map(PassiveProductEntity::getBalance)
                .doOnSuccess(bal -> log.info("[Service] Updated balance for id={} is {}", id, bal))
                .doOnError(err -> log.error("[Service] Error updating balance id={}, err={}", id, err.getMessage()));
    }

    /*
     *   FIND BY CUSTOMER + PRODUCT FILTER + Soft Delete + Logs
     */
    public Flux<PassiveProductEntity> findByCustomerId(String customerId, @Nullable String productId) {
        log.info("[Service] Querying products for customerId={} productId={}", customerId, productId);
        return validateCustomerExists(customerId)
                .doOnSuccess(c -> log.info("[Service] Customer validated: {}", customerId))
                .doOnError(err -> log.error("[Service] Customer validation failed for id={}, error={}", customerId, err.getMessage()))
                .flatMapMany(customer -> {
                    if (productId !=null && !productId.isBlank()){
                        log.info("[Service] Filtering by productId={} for customerId={}", productId, customerId);
                        return repository.findByIdAndCustomerIdAndActiveTrue(productId, customerId)
                                .doOnSuccess(p -> log.info("[Service] Specific product found id={}", p.getId()))
                                .flux()
                                .switchIfEmpty(Mono.error(new BusinessException("Product not found for this customer")));
                    }
                    log.info("[Service] Listing all active products for customerId={}", customerId);
                    return repository.findByCustomerId(customerId)
                            .doOnComplete(() -> log.info("[Service] Completed listing all products for customerId={}", customerId));
                })
                .onErrorResume(err -> {
                    log.error("[Service] Error in findByCustomerId, customerId={}, error={}", customerId, err.getMessage());
                    return Flux.error(err);
                });

    }


    private Mono<Customer> validateCustomerExists(String customerId) {
        return customerWebClient.getCustomer(customerId)
                .switchIfEmpty(Mono.error(new BusinessException("Customer not found")));
    }


    private Mono<Void> validatePersonalCustomer(String customerId, String accountType) {

        return repository.findByCustomerId(customerId)
                .collectList()
                .flatMap(accounts -> {
                    if(accountType.equals(ACCONT_SAVING) &&
                            accounts.stream().anyMatch(a -> a.getAccountType().equals(ACCONT_SAVING))) {
                        return Mono.error(new BusinessException("Personal customer already has a saving account"));
                    }
                    if(accountType.equals(ACCONT_CURRENT) &&
                            accounts.stream().anyMatch(a -> a.getAccountType().equals(ACCONT_CURRENT))) {
                        return Mono.error(new BusinessException("Personal customer already has a current account"));
                    }
                    // FIXED_TERM: allowed multiple – no checks
                    return Mono.empty();
                });
    }


    private Mono<Void> validateBusinessCustomer(String accountType) {

        if(accountType.equals(ACCONT_SAVING) || accountType.equals(ACCONT_FIXED_TERM)) {
            return Mono.error(new BusinessException(
                    "Business customers cannot have saving or fixed-term accounts"
            ));
        }
        return Mono.empty();
    }


    private Mono<PassiveProductEntity> applyAccountRules(PassiveProductEntity e) {

        switch (e.getAccountType()) {
            case ACCONT_SAVING:
                e.setMaintenanceFee(0.0);
                if(e.getTransactionLimit() == null)
                    e.setTransactionLimit(10);
                break;
            case ACCONT_CURRENT:
                if(e.getMaintenanceFee() == null)
                    e.setMaintenanceFee(25.0);
                e.setTransactionLimit(null);
                break;
            case ACCONT_FIXED_TERM:
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
