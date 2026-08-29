package com.smartshop.features.stockTransfer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
public class StockTransferRequest {

    @NotNull(message = "Shop id is required")
    private UUID shopId;

    @NotNull(message = "From branch id is required")
    private UUID fromBranchId;

    @NotNull(message = "To branch id is required")
    private UUID toBranchId;

    @Size(max = 1000, message = "Note must be at most 1000 characters")
    private String note;

    @NotEmpty(message = "At least one transfer item is required")
    @Valid
    private List<StockTransferItemRequest> items;
}