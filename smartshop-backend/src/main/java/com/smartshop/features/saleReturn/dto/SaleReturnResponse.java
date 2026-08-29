package com.smartshop.features.saleReturn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleReturnResponse {

    private UUID id;
    private String returnNumber;
    private LocalDate returnDate;
    private UUID saleId;
    private String invoiceNumber;
    private UUID branchId;
    private String branchName;
    private String reason;
    private BigDecimal refundAmount;
    private UUID createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private List<SaleReturnItemResponse> items;
}