package com.smartshop.features.shop.entity;

import com.smartshop.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "shop_invite_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShopInviteCode extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
