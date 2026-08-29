package com.smartshop.features.product.entity;

import com.smartshop.features.category.entity.Category;
import com.smartshop.features.shop.entity.Shop;
import com.smartshop.features.tax.entity.Tax;
import com.smartshop.shared.entity.BaseEntity;
import com.smartshop.shared.enumeration.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 50)
    private String sku;

    @Column(length = 100)
    private String barcode;

    @Column(length = 100)
    private String brand;

    @Column(nullable = false, length = 20)
    private String unit = "PCS";

    @Column(length = 500)
    private String description;

    @Column(name = "purchase_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Column(name = "selling_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    @Column(name = "vat_applicable", nullable = false)
    private Boolean vatApplicable = true;

    /** Legacy hardcoded rate — used as fallback if tax is null. */
    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate = new BigDecimal("13.00");

    /** Dynamic tax configuration — preferred over vatRate when set. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_id")
    private Tax tax;

    @Column(name = "reorder_level", nullable = false, precision = 12, scale = 2)
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;
}