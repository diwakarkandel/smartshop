package com.smartshop.features.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {

    private UUID id;
    private UUID shopId;
    private String name;
    private String phone;
    private String email;
    private String address;
    private BigDecimal loyaltyPoints;
    private LocalDateTime createdAt;
}