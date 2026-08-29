package com.smartshop.features.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettingsRequest {

    @NotNull(message = "Shop id is required")
    private UUID shopId;

    @NotBlank(message = "Key is required")
    private String key;

    private String value;
}