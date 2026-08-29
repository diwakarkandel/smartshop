package com.smartshop.features.shop.repository;

import com.smartshop.features.shop.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ShopRepository extends JpaRepository<Shop, UUID>, JpaSpecificationExecutor<Shop> {

    boolean existsByPanVatNumber(String panVatNumber);
}