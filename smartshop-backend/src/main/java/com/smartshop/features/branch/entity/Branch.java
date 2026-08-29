package com.smartshop.features.branch.entity;

import com.smartshop.features.shop.entity.Shop;
import com.smartshop.shared.entity.BaseEntity;
import com.smartshop.shared.enumeration.BranchStatus;
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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "branches")
public class Branch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(length = 255)
    private String address;

    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    @Column(name = "is_main_branch", nullable = false)
    private Boolean isMainBranch = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BranchStatus status = BranchStatus.ACTIVE;
}