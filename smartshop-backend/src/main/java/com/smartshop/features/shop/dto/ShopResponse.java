package com.smartshop.features.shop.dto;

import com.smartshop.shared.enumeration.ShopStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopResponse {

    private UUID id;
    private String name;
    private String panVatNumber;
    private String phone;
    private String email;
    private String address;
    private String logoUrl;
    private ShopStatus status;
    private LocalDateTime createdAt;
}