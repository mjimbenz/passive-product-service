package com.bank.passive_product.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "passive_products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassiveProductEntity {

    @Id
    private String id;
    private String customerId;
    private String accountType;
    private Double balance;
    private Integer transactionLimit;
    private Double maintenanceFee;
    private Integer allowedMovementDay;


    // Soft delete + auditoría
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

}
