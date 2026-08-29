package com.smartshop.features.tax.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaxRateRequest {

    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0.00", message = "Rate cannot be negative")
    private BigDecimal rate;

    @NotNull(message = "Valid-from date is required")
    private LocalDate validFrom;

    private LocalDate validTo;
}
