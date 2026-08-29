package com.smartshop.features.branch.dto;

import com.smartshop.shared.enumeration.BranchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchResponse {

    private UUID id;
    private UUID shopId;
    private String shopName;
    private String name;
    private String code;
    private String address;
    private String contactNumber;
    private Boolean isMainBranch;
    private BranchStatus status;
    private LocalDateTime createdAt;
}