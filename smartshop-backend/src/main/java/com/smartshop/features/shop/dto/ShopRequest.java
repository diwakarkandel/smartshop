package com.smartshop.features.shop.dto;

import com.smartshop.shared.enumeration.ShopStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopRequest {

    @NotBlank(message = "Shop name is required")
    @Size(max = 200, message = "Shop name must be at most 200 characters")
    private String name;

    @NotBlank(message = "PAN/VAT number is required")
    @Size(max = 50, message = "PAN/VAT number must be at most 50 characters")
    private String panVatNumber;

    @Size(max = 20, message = "Phone must be at most 20 characters")
    private String phone;

    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    @Size(max = 255, message = "Address must be at most 255 characters")
    private String address;

    @Size(max = 500, message = "Logo URL must be at most 500 characters")
    private String logoUrl;

    private ShopStatus status;
}