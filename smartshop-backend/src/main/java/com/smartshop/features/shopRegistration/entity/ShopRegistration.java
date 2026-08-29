package com.smartshop.features.shopRegistration.entity;

import com.smartshop.features.user.entity.User;
import com.smartshop.shared.entity.BaseEntity;
import com.smartshop.shared.enumeration.RegistrationStatus;
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

@Entity
@Table(name = "shop_registrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShopRegistration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "shop_name", nullable = false, length = 200)
    private String shopName;

    @Column(name = "pan_vat_number", nullable = false, length = 50)
    private String panVatNumber;

    @Column(name = "pan_certificate_url", length = 500)
    private String panCertificateUrl;

    @Column(length = 20)
    private String phone;

    @Column(length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus status = RegistrationStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;
}
