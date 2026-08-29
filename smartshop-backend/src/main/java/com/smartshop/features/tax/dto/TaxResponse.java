package com.smartshop.features.tax.dto;

import com.smartshop.shared.enumeration.TaxType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxResponse {

    private UUID id;
    private UUID shopId;
    private String shopName;
    private String name;
    private TaxType type;
    private String description;
    private Boolean isActive;
    private List<TaxRateResponse> rates;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
