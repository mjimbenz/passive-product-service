package com.bank.passive_product;

import com.bank.passive_product.client.CustomerWebClient;
import com.bank.passive_product.client.entity.Customer;
import com.bank.passive_product.exception.BusinessException;
import com.bank.passive_product.model.PassiveProductEntity;
import com.bank.passive_product.repository.PassiveProductRepository;
import com.bank.passive_product.service.impl.PassiveProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PassiveProductServiceImplTest {


    @Mock
    private PassiveProductRepository repository;

    @Mock
    private CustomerWebClient customerWebClient;

    private PassiveProductServiceImpl service;

    @BeforeEach
    void setup() {
        service = new PassiveProductServiceImpl(repository, customerWebClient);
    }


    private Customer mockCustomer(String id) {
        return Customer.builder()
                .id(id)
                .type("PERSONAL")
                .build();
    }


    /*
    * findAll()
    * */
    @Test
    void findAll_shouldReturnActiveProducts() {

        PassiveProductEntity p1 = new PassiveProductEntity();
        PassiveProductEntity p2 = new PassiveProductEntity();

        Mockito.when(repository.findByActiveTrue())
                .thenReturn(Flux.just(p1, p2));

        StepVerifier.create(service.findAll())
                .expectNext(p1)
                .expectNext(p2)
                .verifyComplete();

        Mockito.verify(repository).findByActiveTrue();
    }

    @Test
    void findById_shouldReturnProduct_whenActive() {
        String id = "123";
        PassiveProductEntity product = new PassiveProductEntity();
        product.setId(id);

        Mockito.when(repository.findByIdAndActiveTrue(id))
                .thenReturn(Mono.just(product));

        StepVerifier.create(service.findById(id))
                .expectNext(product)
                .verifyComplete();

        Mockito.verify(repository).findByIdAndActiveTrue(id);
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        String id = "666";

        Mockito.when(repository.findByIdAndActiveTrue(id))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.findById(id))
                .expectErrorMatches(e ->
                        e instanceof BusinessException &&
                                e.getMessage().contains("Product not found"))
                .verify();

        Mockito.verify(repository).findByIdAndActiveTrue(id);
    }


    @Test
    void create_shouldCreatePersonalSavingAccount() {
        PassiveProductEntity e = new PassiveProductEntity();
        e.setCustomerId("CUST1");
        e.setAccountType("SAVING");

        Customer customer = Customer.builder()
                .id("CUST1")
                .type("PERSONAL")
                .build();

        Mockito.when(customerWebClient.getCustomer("CUST1"))
                .thenReturn(Mono.just(customer));

        Mockito.when(repository.findByCustomerId("CUST1"))
                .thenReturn(Flux.empty());

        Mockito.when(repository.save(Mockito.any()))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.create(e))
                .assertNext(result -> {
                    assertNotNull(result.getCreatedAt());
                    assertEquals("SAVING", result.getAccountType());
                    assertEquals(0.0, result.getBalance());
                })
                .verifyComplete();
    }

    @Test
    void create_shouldFailForFixedTermWithoutMovementDay() {
        PassiveProductEntity e = new PassiveProductEntity();
        e.setCustomerId("CUST1");
        e.setAccountType("FIXED_TERM");

        Customer customer = Customer.builder()
                .id("CUST1")
                .type("PERSONAL")
                .build();

        Mockito.when(customerWebClient.getCustomer("CUST1"))
                .thenReturn(Mono.just(customer));

        Mockito.when(repository.findByCustomerId("CUST1"))
                .thenReturn(Flux.empty());

        StepVerifier.create(service.create(e))
                .expectErrorMatches(ex ->
                        ex instanceof BusinessException &&
                                ex.getMessage().contains("allowedMovementDay"))
                .verify();
    }

    @Test
    void update_shouldUpdateWhenValid() {
        String id = "123";

        PassiveProductEntity existing = new PassiveProductEntity();
        existing.setId(id);
        existing.setCustomerId("CUST1");
        existing.setAccountType("SAVING");
        existing.setActive(true);

        PassiveProductEntity request = new PassiveProductEntity();
        request.setAccountType("SAVING");

        Customer customer = Customer.builder()
                .id("CUST1")
                .type("PERSONAL")
                .build();

        Mockito.when(repository.findByIdAndActiveTrue(id))
                .thenReturn(Mono.just(existing));

        Mockito.when(customerWebClient.getCustomer("CUST1"))
                .thenReturn(Mono.just(customer));

        Mockito.when(repository.findByCustomerId("CUST1"))
                .thenReturn(Flux.empty());

        Mockito.when(repository.save(Mockito.any()))
                .thenReturn(Mono.just(existing));

        StepVerifier.create(service.update(id, request))
                .expectNext(existing)
                .verifyComplete();
    }


    @Test
    void delete_shouldSoftDelete() {
        PassiveProductEntity existing = new PassiveProductEntity();
        existing.setId("123");
        existing.setActive(true);

        Mockito.when(repository.findById("123"))
                .thenReturn(Mono.just(existing));

        Mockito.when(repository.save(existing))
                .thenReturn(Mono.just(existing));

        StepVerifier.create(service.delete("123"))
                .verifyComplete();

        assertFalse(existing.isActive());
    }

    @Test
    void getBalance_shouldReturnBalance_whenProductExists() {
        String id = "123";

        PassiveProductEntity entity = new PassiveProductEntity();
        entity.setId(id);
        entity.setBalance(500.0);

        Mockito.when(repository.findById(id))
                .thenReturn(Mono.just(entity));

        StepVerifier.create(service.getBalance(id))
                .expectNext(500.0)
                .verifyComplete();

        Mockito.verify(repository).findById(id);
    }


    @Test
    void getBalance_shouldCompleteEmpty_whenProductNotFound() {
        String id = "999";

        Mockito.when(repository.findById(id))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.getBalance(id))
                .verifyComplete();

        Mockito.verify(repository).findById(id);
    }

    @Test
    void findByCustomerId_shouldReturnSpecificProduct_whenProductIdProvided() {
        String customerId = "CUST1";
        String productId = "P123";

        Customer customer = mockCustomer(customerId);

        PassiveProductEntity product = new PassiveProductEntity();
        product.setId(productId);
        product.setCustomerId(customerId);
        product.setActive(true);

        Mockito.when(customerWebClient.getCustomer(customerId))
                .thenReturn(Mono.just(customer));

        Mockito.when(repository.findByIdAndCustomerIdAndActiveTrue(productId, customerId))
                .thenReturn(Mono.just(product));

        StepVerifier.create(service.findByCustomerId(customerId, productId))
                .expectNext(product)
                .verifyComplete();

        Mockito.verify(repository).findByIdAndCustomerIdAndActiveTrue(productId, customerId);
    }


    @Test
    void findByCustomerId_shouldReturnAllProducts_whenNoProductIdProvided() {
        String customerId = "CUST1";

        PassiveProductEntity p1 = new PassiveProductEntity();
        p1.setId("P1");
        p1.setCustomerId(customerId);

        PassiveProductEntity p2 = new PassiveProductEntity();
        p2.setId("P2");
        p2.setCustomerId(customerId);

        Mockito.when(customerWebClient.getCustomer(customerId))
                .thenReturn(Mono.just(mockCustomer(customerId)));

        Mockito.when(repository.findByCustomerId(customerId))
                .thenReturn(Flux.just(p1, p2));

        StepVerifier.create(service.findByCustomerId(customerId, null))
                .expectNext(p1)
                .expectNext(p2)
                .verifyComplete();

        Mockito.verify(repository).findByCustomerId(customerId);
    }




}
