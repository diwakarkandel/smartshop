package com.smartshop.features.shop.dto;

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
public class InviteCodeResponse {

    private UUID shopId;
    private String code;
    private boolean isActive;
    private LocalDateTime createdAt;
}
