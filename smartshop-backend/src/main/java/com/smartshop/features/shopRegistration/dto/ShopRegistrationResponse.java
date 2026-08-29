package com.smartshop.features.shopRegistration.dto;

import com.smartshop.shared.enumeration.RegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopRegistrationResponse {

    private UUID id;
    private UUID userId;
    private String applicantName;
    private String applicantEmail;
    private String shopName;
    private String panVatNumber;
    private String panCertificateUrl;
    private String phone;
    private String address;
    private RegistrationStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;
}
