package com.smartshop.features.branch.dto;

import com.smartshop.shared.enumeration.BranchStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchRequest {

    @NotNull(message = "Shop id is required")
    private UUID shopId;

    @NotBlank(message = "Branch name is required")
    @Size(max = 200, message = "Branch name must be at most 200 characters")
    private String name;

    @NotBlank(message = "Branch code is required")
    @Size(max = 20, message = "Branch code must be at most 20 characters")
    private String code;

    @Size(max = 255, message = "Address must be at most 255 characters")
    private String address;

    @Size(max = 20, message = "Contact number must be at most 20 characters")
    private String contactNumber;

    private Boolean isMainBranch;

    private BranchStatus status;
}