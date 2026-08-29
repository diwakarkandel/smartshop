package com.smartshop.features.tax.dto;

import com.smartshop.shared.enumeration.TaxType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaxRequest {

    @NotNull(message = "Shop ID is required")
    private UUID shopId;

    @NotBlank(message = "Tax name is required")
    @Size(max = 100, message = "Tax name must be at most 100 characters")
    private String name;

    @NotNull(message = "Tax type is required")
    private TaxType type;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    private Boolean isActive = true;

    @Valid
    private List<TaxRateRequest> rates;
}
