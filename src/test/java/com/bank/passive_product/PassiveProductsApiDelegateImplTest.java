package com.bank.passive_product;
import com.bank.passive_product.api.model.BalanceResponse;
import com.bank.passive_product.api.model.PassiveProduct;
import com.bank.passive_product.api.model.PassiveProductRequest;
import com.bank.passive_product.controller.PassiveProductsApiDelegateImpl;
import com.bank.passive_product.model.*;
import com.bank.passive_product.service.PassiveProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

class PassiveProductsApiDelegateImplTest {

    private PassiveProductService service;
    private PassiveProductsApiDelegateImpl delegate;

    @BeforeEach
    void setup() {
        service = Mockito.mock(PassiveProductService.class);
        delegate = new PassiveProductsApiDelegateImpl(service);
    }

    // ------------------------------------------------------------------------
    // rootGet (list all)
    // ------------------------------------------------------------------------
    @Test
    void rootGet_shouldReturnFluxOfModels() {
        PassiveProductEntity e1 = new PassiveProductEntity();
        e1.setId("P1");
        e1.setCustomerId("C1");
        e1.setAccountType("SAVING");
        e1.setBalance(100.0);
        e1.setMaintenanceFee(0.0);
        e1.setTransactionLimit(10);

        Mockito.when(service.findAll())
                .thenReturn(Flux.just(e1));

        StepVerifier.create(delegate.rootGet(null))
                .assertNext(response -> {
                    Flux<PassiveProduct> body = response.getBody();
                    StepVerifier.create(body)
                            .assertNext(p -> {
                                assert p.getId().equals("P1");
                                assert p.getCustomerId().equals("C1");
                            })
                            .verifyComplete();
                })
                .verifyComplete();

        Mockito.verify(service).findAll();
    }

    // ------------------------------------------------------------------------
    // idGet
    // ------------------------------------------------------------------------
    @Test
    void idGet_shouldReturnSingleProduct() {
        PassiveProductEntity e = new PassiveProductEntity();
        e.setId("P1");
        e.setCustomerId("C1");
        e.setBalance(200.0);
        e.setAccountType("CURRENT");
        e.setMaintenanceFee(10.0);

        Mockito.when(service.findById("P1")).thenReturn(Mono.just(e));

        StepVerifier.create(delegate.idGet("P1", null))
                .assertNext(response -> {
                    PassiveProduct p = response.getBody();
                    assert p.getId().equals("P1");
                    assert p.getCustomerId().equals("C1");
                })
                .verifyComplete();
    }

    // ------------------------------------------------------------------------
    // rootPost (create)
    // ------------------------------------------------------------------------
    @Test
    void rootPost_shouldCreateProduct() {

        PassiveProductRequest req = new PassiveProductRequest()
                .customerId("C1")
                .accountType(PassiveProductRequest.AccountTypeEnum.SAVING);

        PassiveProductEntity saved = new PassiveProductEntity();
        saved.setId("P99");
        saved.setCustomerId("C1");
        saved.setAccountType("SAVING");
        saved.setBalance(0.0);
        saved.setMaintenanceFee(0.0);
        saved.setTransactionLimit(10);

        Mockito.when(service.create(Mockito.any()))
                .thenReturn(Mono.just(saved));

        StepVerifier.create(delegate.rootPost(Mono.just(req), null))
                .assertNext(resp -> {
                    PassiveProduct body = resp.getBody();
                    assert body.getId().equals("P99");
                    assert body.getCustomerId().equals("C1");
                })
                .verifyComplete();
    }

    // ------------------------------------------------------------------------
    // idDelete
    // ------------------------------------------------------------------------
    @Test
    void idDelete_shouldReturnNoContent() {

        Mockito.when(service.delete("P1"))
                .thenReturn(Mono.empty());

        StepVerifier.create(delegate.idDelete("P1", null))
                .assertNext(resp -> {
                    assert resp.getStatusCode().is2xxSuccessful();
                })
                .verifyComplete();
    }

    // ------------------------------------------------------------------------
    // idPut (update)
    // ------------------------------------------------------------------------
    @Test
    void idPut_shouldUpdateAndReturnNoContent() {

        PassiveProductRequest req = new PassiveProductRequest()
                .customerId("C1")
                .accountType(PassiveProductRequest.AccountTypeEnum.CURRENT);

        Mockito.when(service.update(Mockito.eq("P1"), Mockito.any()))
                .thenReturn(Mono.just(new PassiveProductEntity()));

        StepVerifier.create(delegate.idPut("P1", Mono.just(req), null))
                .assertNext(resp -> {
                    assert resp.getStatusCode().is2xxSuccessful();
                })
                .verifyComplete();
    }

    // ------------------------------------------------------------------------
    // idBalanceGet
    // ------------------------------------------------------------------------
    @Test
    void idBalanceGet_shouldReturnBalance() {

        Mockito.when(service.getBalance("P1"))
                .thenReturn(Mono.just(350.0));

        StepVerifier.create(delegate.idBalanceGet("P1", null))
                .assertNext(resp -> {
                    BalanceResponse bal = resp.getBody();
                    assert bal.getBalance().equals(BigDecimal.valueOf(350.0));
                })
                .verifyComplete();
    }

    // ------------------------------------------------------------------------
    // customerCustomerIdGet
    // ------------------------------------------------------------------------
    @Test
    void customerCustomerIdGet_shouldReturnProducts() {

        PassiveProductEntity e = new PassiveProductEntity();
        e.setId("PX");
        e.setCustomerId("CUST1");
        e.setBalance(50.0);
        e.setAccountType("SAVING");
        e.setMaintenanceFee(0.0);
        e.setTransactionLimit(10);

        Mockito.when(service.findByCustomerId("CUST1", null))
                .thenReturn(Flux.just(e));

        StepVerifier.create(delegate.customerCustomerIdGet("CUST1", null, null))
                .assertNext(resp -> {
                    Flux<PassiveProduct> flux = resp.getBody();
                    StepVerifier.create(flux)
                            .assertNext(p -> {
                                assert p.getId().equals("PX");
                                assert p.getCustomerId().equals("CUST1");
                            })
                            .verifyComplete();
                })
                .verifyComplete();
    }
}
