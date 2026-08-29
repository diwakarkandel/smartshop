package com.smartshop.features.tax.repository;

import com.smartshop.features.tax.entity.Tax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxRepository extends JpaRepository<Tax, UUID> {

    List<Tax> findByShopId(UUID shopId);

    List<Tax> findByShopIdAndIsActiveTrue(UUID shopId);

    Optional<Tax> findByShopIdAndName(UUID shopId, String name);

    boolean existsByShopIdAndNameAndIdNot(UUID shopId, String name, UUID id);
}
