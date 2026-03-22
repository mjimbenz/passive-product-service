package com.bank.passive_product.exception.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorResponse {

    private String timestamp;
    private String errorCode;
    private String message;
    private String path;
    private String microservice = "passive-product-service";
}
