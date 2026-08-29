package com.smartshop.features.supplier.dto;

import com.smartshop.shared.enumeration.Status;
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
public class SupplierResponse {

    private UUID id;
    private UUID shopId;
    private String name;
    private String companyName;
    private String phone;
    private String email;
    private String address;
    private String panNumber;
    private Status status;
    private LocalDateTime createdAt;
}