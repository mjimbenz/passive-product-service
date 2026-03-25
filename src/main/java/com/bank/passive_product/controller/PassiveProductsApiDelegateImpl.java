package com.bank.passive_product.controller;

import com.bank.passive_product.api.PassiveProductApiDelegate;
import com.bank.passive_product.api.model.BalanceResponse;
import com.bank.passive_product.api.model.BalanceUpdateRequest;
import com.bank.passive_product.api.model.PassiveProduct;
import com.bank.passive_product.api.model.PassiveProductRequest;
import com.bank.passive_product.model.PassiveProductEntity;
import com.bank.passive_product.service.PassiveProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class PassiveProductsApiDelegateImpl implements PassiveProductApiDelegate {

    private final PassiveProductService service;


    /* Get All passive product*/
    @Override
    public Mono<ResponseEntity<Flux<PassiveProduct>>> rootGet(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(service.findAll().map(this::toModel)));

    }

    @Override
    public Mono<ResponseEntity<PassiveProduct>> idGet(String id, ServerWebExchange exchange) {
        return service.findById(id)
                .map(this::toModel)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<PassiveProduct>> rootPost(Mono<PassiveProductRequest> passiveProductRequest, ServerWebExchange exchange) {
        log.info("[api] Creating product -> {}", passiveProductRequest);
        return passiveProductRequest.map(this::toEntity)
                .flatMap(service::create)
                .doOnSuccess(c -> log.info("[api] Customer validated for create, id={}",c.getCustomerId()))
                .doOnError(err -> log.error("[api] Customer validation failed for create, error={}", err.getMessage()))
                .map(this::toModel)
                .map(ResponseEntity::ok);
    }


    @Override
    public Mono<ResponseEntity<Void>> idDelete(String id, ServerWebExchange exchange) {
        return service.delete(id)
                .thenReturn(ResponseEntity.noContent().build());
    }


    @Override
    public Mono<ResponseEntity<Void>> idPut(String id, Mono<PassiveProductRequest> passiveProductRequest, ServerWebExchange exchange) {
        return passiveProductRequest
                .flatMap(request -> service.update(id, toEntity(request)))
                .thenReturn(ResponseEntity.noContent().build());
    }

    @Override
    public Mono<ResponseEntity<BalanceResponse>> idBalanceGet(String id, ServerWebExchange exchange) {
        return service.getBalance(id)
                .map(balance -> new BalanceResponse().balance(BigDecimal.valueOf(balance)))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<PassiveProduct>>> customerCustomerIdGet(String customerId, String id, ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(service.findByCustomerId(customerId, id).map(this::toModel)));
    }

    @Override
    public Mono<ResponseEntity<PassiveProduct>> idBalancePatch(String id, Mono<BalanceUpdateRequest> balanceUpdateRequest, ServerWebExchange exchange) {
        log.info("[api] Updating balance for passive product with id={} -> {}", id, balanceUpdateRequest);
        return balanceUpdateRequest
                .flatMap(request -> {
                    log.info("[api] Validating balance update for passive product with id={} -> {}", id, request);
                    return service.updateBalance(id, request.getAmount().doubleValue())
                            .flatMap(e -> service.findById(id));
                })
                .map(this::toModel)
                .map(ResponseEntity::ok)
                .doOnSuccess(c -> log.info("[api] Successfully updated balance for passive product with id={}", id))
                .doOnError(err -> log.error("[api] Error updating balance for passive product with id={}, error={}", id, err.getMessage()));

    }

    private PassiveProductEntity toEntity(PassiveProductRequest r) {
        return PassiveProductEntity.builder()
                .customerId(r.getCustomerId())
                .accountType(r.getAccountType().toString())
                .balance(0.0)
                .build();
    }

    private PassiveProduct toModel(PassiveProductEntity e) {
        return new PassiveProduct()
                .id(e.getId())
                .customerId(e.getCustomerId())
                .accountType(PassiveProduct.AccountTypeEnum.fromValue(e.getAccountType()))
                .balance(BigDecimal.valueOf(e.getBalance()))
                .transactionLimit(e.getTransactionLimit())
                .active(e.isActive())
                .maintenanceFee(BigDecimal.valueOf(e.getMaintenanceFee()))
                .allowedMovementDay(e.getAllowedMovementDay());
    }

}
