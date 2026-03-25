package com.bank.passive_product.service.saga;

import com.bank.events.MovementCompletedEvent;
import com.bank.events.MovementRejectedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MovementResponsePublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCompleted(String movementId) {
        MovementCompletedEvent event = MovementCompletedEvent.newBuilder()
                .setMovementId(movementId)
                .setStatus("COMPLETED")
                .build();

        kafkaTemplate.send("movement-completed", movementId, event);
    }

    public void publishRejected(String movementId, String reason) {
        MovementRejectedEvent event = MovementRejectedEvent.newBuilder()
                .setMovementId(movementId)
                .setReason(reason)
                .build();

        kafkaTemplate.send("movement-rejected", movementId, event);
    }
}
