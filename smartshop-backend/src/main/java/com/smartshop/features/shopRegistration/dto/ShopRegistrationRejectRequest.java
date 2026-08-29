package com.smartshop.features.shopRegistration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopRegistrationRejectRequest {

    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;
}
