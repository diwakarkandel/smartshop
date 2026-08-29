package com.smartshop.features.inventory.entity;

import com.smartshop.features.branch.entity.Branch;
import com.smartshop.features.product.entity.Product;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventories", uniqueConstraints = {
        @UniqueConstraint(name = "uq_inventories_branch_product", columnNames = {"branch_id", "product_id"})
})
public class Inventory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity_available", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityAvailable = BigDecimal.ZERO;

    @Column(name = "quantity_reserved", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityReserved = BigDecimal.ZERO;

    @Column(name = "average_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal averageCost = BigDecimal.ZERO;

    @Column(name = "last_stock_update")
    private LocalDateTime lastStockUpdate;
}