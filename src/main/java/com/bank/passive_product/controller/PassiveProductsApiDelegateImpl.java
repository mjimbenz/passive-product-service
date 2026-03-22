package com.bank.passive_product.controller;

import com.bank.passive_product.api.PassiveProductApiDelegate;
import com.bank.passive_product.api.model.BalanceResponse;
import com.bank.passive_product.api.model.PassiveProduct;
import com.bank.passive_product.api.model.PassiveProductRequest;
import com.bank.passive_product.model.PassiveProductEntity;
import com.bank.passive_product.service.PassiveProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
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
        return passiveProductRequest.map(this::toEntity)
                .flatMap(service::create)
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

    private PassiveProductEntity toEntity(PassiveProductRequest r) {
        return PassiveProductEntity.builder()
                .customerId(r.getCustomerId())
                .accountType(r.getAccountType().getValue())
                .allowedMovementDay(r.getAllowedMovementDay())
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
                .maintenanceFee(BigDecimal.valueOf(e.getMaintenanceFee()))
                .allowedMovementDay(e.getAllowedMovementDay());
    }

}
