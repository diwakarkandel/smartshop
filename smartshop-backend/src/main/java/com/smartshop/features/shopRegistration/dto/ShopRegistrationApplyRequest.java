package com.smartshop.features.shopRegistration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopRegistrationApplyRequest {

    @NotBlank(message = "Shop name is required")
    @Size(max = 200, message = "Shop name must be at most 200 characters")
    private String shopName;

    @NotBlank(message = "PAN/VAT number is required")
    @Size(max = 50, message = "PAN/VAT number must be at most 50 characters")
    private String panVatNumber;

    @NotBlank(message = "PAN certificate upload is required")
    @Size(max = 500, message = "PAN certificate URL must be at most 500 characters")
    private String panCertificateUrl;

    @Size(max = 20, message = "Phone must be at most 20 characters")
    private String phone;

    @Size(max = 500, message = "Address must be at most 500 characters")
    private String address;
}
