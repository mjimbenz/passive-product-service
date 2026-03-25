package com.bank.passive_product.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "passive_products")
public class PassiveProductEntity {

    @Id
    @NotBlank(message = "El ID del producto no puede estar vacío")
    private String id;

    @NotBlank(message = "El ID del cliente es obligatorio")
    private String customerId;

    @NotBlank(message = "El tipo de cuenta es obligatorio")
    private String accountType;  // Ej: SAVINGS, CURRENT

    @NotNull(message = "El saldo es obligatorio")
    @PositiveOrZero(message = "El saldo no puede ser negativo")
    private Double balance;

    @NotNull(message = "El límite de transacciones es obligatorio")
    @Positive(message = "El límite de transacciones debe ser mayor a 0")
    private Integer transactionLimit;

    @NotNull(message = "La comisión de mantenimiento es obligatoria")
    @PositiveOrZero(message = "La comisión de mantenimiento no puede ser negativa")
    private Double maintenanceFee;

    @NotNull(message = "Los días permitidos de movimiento son obligatorios")
    @Min(value = 1, message = "El día permitido mínimo es 1")
    @Max(value = 31, message = "El día permitido máximo es 31")
    private Integer allowedMovementDay;

    // Auditoría y Soft Delete
    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}