package com.smartshop.features.stockTransfer.dto;

import com.smartshop.shared.enumeration.TransferStatus;
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
public class StockTransferResponse {

    private UUID id;
    private String transferNumber;
    private UUID shopId;
    private UUID fromBranchId;
    private String fromBranchName;
    private UUID toBranchId;
    private String toBranchName;
    private TransferStatus status;
    private String note;
    private UUID createdById;
    private String createdByName;
    private UUID approvedById;
    private String approvedByName;
    private LocalDateTime createdAt;
    private List<StockTransferItemResponse> items;
}