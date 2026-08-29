package com.smartshop.features.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequest {

    @NotNull(message = "Shop id is required")
    private UUID shopId;

    @NotBlank(message = "Customer name is required")
    @Size(max = 200, message = "Customer name must be at most 200 characters")
    private String name;

    @Size(max = 20, message = "Phone must be at most 20 characters")
    private String phone;

    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    @Size(max = 255, message = "Address must be at most 255 characters")
    private String address;
}