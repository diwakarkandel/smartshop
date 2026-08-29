package com.smartshop.features.sale.dto;

import com.smartshop.shared.enumeration.PaymentMethod;
import com.smartshop.shared.enumeration.PaymentStatus;
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
public class SaleResponse {

    private UUID id;
    private String invoiceNumber;
    private LocalDate billDate;
    private UUID shopId;
    private UUID branchId;
    private String branchName;
    private String branchCode;
    private UUID customerId;
    private String customerName;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxableAmount;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private String qrReference;
    private BigDecimal cashTendered;
    private BigDecimal changeAmount;
    private UUID cashierId;
    private String cashierName;
    private String remarks;
    private LocalDateTime createdAt;
    private List<SaleItemResponse> items;
}