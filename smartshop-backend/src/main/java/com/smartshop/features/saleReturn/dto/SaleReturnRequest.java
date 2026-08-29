package com.smartshop.features.saleReturn.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleReturnRequest {

    @NotNull(message = "Sale id is required")
    private UUID saleId;

    @NotNull(message = "Branch id is required")
    private UUID branchId;

    private LocalDate returnDate;

    @Size(max = 1000, message = "Reason must be at most 1000 characters")
    private String reason;

    @NotEmpty(message = "At least one return item is required")
    @Valid
    private List<SaleReturnItemRequest> items;
}