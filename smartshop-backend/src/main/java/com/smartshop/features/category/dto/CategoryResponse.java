package com.smartshop.features.category.dto;

import com.smartshop.shared.enumeration.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private UUID id;
    private UUID shopId;
    private UUID parentId;
    private String name;
    private String code;
    private String description;
    private Status status;
    private LocalDateTime createdAt;
    private List<CategoryResponse> children;

    public static void appendChild(CategoryResponse parent, CategoryResponse child) {
        if (parent.getChildren() == null) {
            parent.setChildren(new ArrayList<>());
        }
        parent.getChildren().add(child);
    }
}