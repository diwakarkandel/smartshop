package com.smartshop.features.category.dto;

import com.smartshop.shared.enumeration.Status;
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
public class CategoryRequest {

    @NotNull(message = "Shop id is required")
    private UUID shopId;

    private UUID parentId;

    @NotBlank(message = "Category name is required")
    @Size(max = 200, message = "Category name must be at most 200 characters")
    private String name;

    @Size(max = 50, message = "Category code must be at most 50 characters")
    private String code;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    private Status status;
}