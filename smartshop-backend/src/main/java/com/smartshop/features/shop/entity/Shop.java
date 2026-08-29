package com.smartshop.features.shop.entity;

import com.smartshop.shared.entity.BaseEntity;
import com.smartshop.shared.enumeration.ShopStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shops")
public class Shop extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "pan_vat_number", nullable = false, unique = true, length = 50)
    private String panVatNumber;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(length = 255)
    private String address;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShopStatus status = ShopStatus.ACTIVE;
}