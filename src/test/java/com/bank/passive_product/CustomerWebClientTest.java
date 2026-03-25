//package com.bank.passive_product;
//
//import com.bank.passive_product.client.CustomerWebClient;
//import com.bank.passive_product.exception.BusinessException;
//import okhttp3.mockwebserver.MockResponse;
//import okhttp3.mockwebserver.MockWebServer;
//import org.junit.jupiter.api.*;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.test.StepVerifier;
//
//class CustomerWebClientTest {
//
//    private MockWebServer mockWebServer;
//    private CustomerWebClient customerWebClient;
//
//    @BeforeEach
//    void setUp() throws Exception {
//        mockWebServer = new MockWebServer();
//        mockWebServer.start();
//
//        WebClient client = WebClient.builder()
//                .baseUrl(mockWebServer.url("/").toString())
//                .build();
//
//        customerWebClient = new CustomerWebClient(client);
//    }
//
//    @AfterEach
//    void cleanUp() throws Exception {
//        mockWebServer.shutdown();
//    }
//
//    // --------------------------------------------------------------------------------------------
//    // ✔ TEST 1: Servicio responde 200 OK correctamente
//    // --------------------------------------------------------------------------------------------
//    @Test
//    void getCustomer_shouldReturnCustomer_whenStatus200() {
//
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(200)
//                .setBody("{\"id\":\"123\", \"type\":\"PERSONAL\"}")
//                .addHeader("Content-Type", "application/json")
//        );
//
//        StepVerifier.create(customerWebClient.getCustomer("123"))
//                .expectNextMatches(c ->
//                        c.id().equals("123") &&
//                                c.type().equals("PERSONAL")
//                )
//                .verifyComplete();
//    }
//
//    // --------------------------------------------------------------------------------------------
//    // ✔ TEST 2: Servicio falla → circuit breaker → fallback → BusinessException
//    // --------------------------------------------------------------------------------------------
//    @Test
//    void getCustomer_shouldTriggerFallback_whenStatus500() {
//
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(500)
//                .setBody("Internal Server Error")
//        );
//
//        StepVerifier.create(
//                        customerWebClient.getCustomer("123")
//                                .onErrorResume(ex -> customerWebClient.fallbackCustomer("123", ex))
//                )
//                .expectErrorMatches(BusinessException.class::isInstance )
//                .verify();
//    }
//
//
//    // --------------------------------------------------------------------------------------------
//    // ✔ TEST 3: Respuesta 200 pero body vacío → decoding error → fallback
//    // --------------------------------------------------------------------------------------------
//    @Test
//    void getCustomer_shouldFallback_whenBodyIsEmpty() {
//
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(200)
//                .setBody("")
//                .addHeader("Content-Type", "application/json")
//        );
//
//        StepVerifier.create(customerWebClient.getCustomer("123"))
//                .expectError(BusinessException.class)
//                .verify();
//    }
//
//}
//
