package com.smartshop.features.tax.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxRateResponse {

    private UUID id;
    private UUID taxId;
    private BigDecimal rate;
    private LocalDate validFrom;
    private LocalDate validTo;
    private LocalDateTime createdAt;
}
