package com.smartshop.features.product.repository;

import com.smartshop.features.product.entity.Product;
import com.smartshop.shared.enumeration.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    boolean existsByShopIdAndSku(UUID shopId, String sku);

    boolean existsByCategoryId(UUID categoryId);

    Optional<Product> findFirstByShopIdAndBarcode(UUID shopId, String barcode);

    List<Product> findByShopIdAndStatusOrderByNameAsc(UUID shopId, Status status);
}