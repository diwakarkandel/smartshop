package com.smartshop.features.expense.dto;

import com.smartshop.shared.enumeration.PaymentMethod;
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
public class ExpenseResponse {

    private UUID id;
    private UUID shopId;
    private UUID branchId;
    private String branchName;
    private String title;
    private String category;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private PaymentMethod paymentMethod;
    private String note;
    private UUID createdById;
    private String createdByName;
    private LocalDateTime createdAt;
}