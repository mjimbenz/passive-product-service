package com.bank.passive_product.service.saga;

import com.bank.events.MovementRequestedEvent;
import com.bank.passive_product.service.PassiveProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@EnableKafka
@Service
@RequiredArgsConstructor
@Slf4j
public class MovementRequestedConsumer {

    private final PassiveProductService service;
    private final MovementResponsePublisher publisher;

    @KafkaListener(
            topics = "movement-requested",
            groupId = "passive-product-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void listen(MovementRequestedEvent event) {
        log.info("Received MovementRequestedEvent for movementId: {}, productId: {}, amount: {}",
                event.getMovementId(), event.getProductId(), event.getAmount());
        if(event.getMovementType().equals("DEPOSIT")){
            log.info("Processing DEPOSIT movement for productId: {}, amount: {}",
                    event.getProductId(), event.getAmount());
            service.updateBalance(
                            event.getProductId(),
                            event.getAmount()
                    )
                    .doOnSuccess(v -> publisher.publishCompleted(event.getMovementId()))
                    .doOnError(err -> publisher.publishRejected(event.getMovementId(), err.getMessage()))
                    .subscribe();
        } else {
            log.info("Processing WITHDRALL movement for productId: {}, amount: {}",
                    event.getProductId(), event.getAmount());
            service.updateBalance(
                            event.getProductId(),
                            -event.getAmount()
                    )
                    .doOnSuccess(v -> publisher.publishCompleted(event.getMovementId()))
                    .doOnError(err -> publisher.publishRejected(event.getMovementId(), err.getMessage()))
                    .subscribe();
        }
    }
}