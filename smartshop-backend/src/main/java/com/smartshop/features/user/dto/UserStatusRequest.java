package com.smartshop.features.user.dto;

import com.smartshop.shared.enumeration.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusRequest {

    @NotNull(message = "Status is required")
    private UserStatus status;
}