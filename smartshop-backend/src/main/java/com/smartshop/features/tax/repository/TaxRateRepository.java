package com.smartshop.features.tax.repository;

import com.smartshop.features.tax.entity.TaxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxRateRepository extends JpaRepository<TaxRate, UUID> {

    List<TaxRate> findByTaxIdOrderByValidFromDesc(UUID taxId);

    /**
     * Returns the rate effective on a given date.
     * Picks the most-recently-started rate where validFrom <= date AND (validTo IS NULL OR validTo >= date).
     */
    @Query("""
           SELECT tr FROM TaxRate tr
           WHERE tr.tax.id = :taxId
             AND tr.validFrom <= :date
             AND (tr.validTo IS NULL OR tr.validTo >= :date)
           ORDER BY tr.validFrom DESC
           """)
    Optional<TaxRate> findActiveRateByTaxIdAndDate(@Param("taxId") UUID taxId, @Param("date") LocalDate date);

    boolean existsByTaxId(UUID taxId);
}
