package com.smartshop.features.payment.repository;

import com.smartshop.features.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {

    List<Payment> findBySaleId(UUID saleId);

    @Query("SELECT p.paymentMethod, COALESCE(SUM(p.amount), 0) FROM Payment p "
            + "WHERE p.sale.shop.id = :shopId AND p.paidAt BETWEEN :from AND :to "
            + "GROUP BY p.paymentMethod")
    List<Object[]> sumByShopAndMethod(@Param("shopId") UUID shopId,
                                      @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}