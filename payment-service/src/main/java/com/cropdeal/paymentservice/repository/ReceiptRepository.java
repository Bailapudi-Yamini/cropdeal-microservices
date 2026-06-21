package com.cropdeal.paymentservice.repository;

import com.cropdeal.paymentservice.entity.Receipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    Optional<Receipt> findByPaymentId(Long paymentId);

    Optional<Receipt> findByReceiptNumber(String receiptNumber);

    Page<Receipt> findByFarmerId(Long farmerId, Pageable pageable);

    Page<Receipt> findByDealerId(Long dealerId, Pageable pageable);

    /** Highest sequence number for the current year — used for receipt number generation */
    @Query("SELECT COUNT(r) FROM Receipt r WHERE YEAR(r.generatedAt) = :year")
    long countByYear(@Param("year") int year);
}
