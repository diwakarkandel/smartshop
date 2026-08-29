package com.smartshop.features.settings.dto;

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
public class SettingsResponse {

    private UUID id;
    private UUID shopId;
    private String settingKey;
    private String settingValue;
    private String description;
    private LocalDateTime updatedAt;
}