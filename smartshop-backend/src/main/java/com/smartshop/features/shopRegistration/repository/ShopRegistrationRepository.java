package com.smartshop.features.shopRegistration.repository;

import com.smartshop.features.shopRegistration.entity.ShopRegistration;
import com.smartshop.shared.enumeration.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ShopRegistrationRepository extends JpaRepository<ShopRegistration, UUID> {

    boolean existsByUserIdAndStatusIn(UUID userId, Collection<RegistrationStatus> statuses);

    Optional<ShopRegistration> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<ShopRegistration> findByStatus(RegistrationStatus status, Pageable pageable);
}
