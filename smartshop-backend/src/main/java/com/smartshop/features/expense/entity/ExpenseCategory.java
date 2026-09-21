package com.smartshop.features.expense.entity;

import com.smartshop.features.shop.entity.Shop;
import com.smartshop.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "expense_categories",
        uniqueConstraints = @UniqueConstraint(columnNames = {"shop_id", "name"}))
public class ExpenseCategory extends BaseEntity {

    /** Null means this is a global/default category visible to all shops. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id")
    private Shop shop;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;
}