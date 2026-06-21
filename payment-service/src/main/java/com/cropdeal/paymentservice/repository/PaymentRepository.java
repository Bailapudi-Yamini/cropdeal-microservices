package com.cropdeal.paymentservice.repository;

import com.cropdeal.paymentservice.entity.Payment;
import com.cropdeal.paymentservice.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByTransactionId(String transactionId);

    boolean existsByOrderId(Long orderId);

    Page<Payment> findByDealerId(Long dealerId, Pageable pageable);

    Page<Payment> findByFarmerId(Long farmerId, Pageable pageable);

    Page<Payment> findByDealerIdAndStatus(Long dealerId, PaymentStatus status, Pageable pageable);

    Page<Payment> findByFarmerIdAndStatus(Long farmerId, PaymentStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE Payment p SET p.status = :status, p.paymentGatewayRef = :ref, " +
           "p.gatewayResponse = :response, p.paidAt = CURRENT_TIMESTAMP WHERE p.id = :id")
    int markSuccess(@Param("id") Long id,
                    @Param("ref") String gatewayRef,
                    @Param("response") String gatewayResponse);

    @Modifying
    @Query("UPDATE Payment p SET p.status = :status, p.gatewayResponse = :response WHERE p.id = :id")
    int updateStatusWithResponse(@Param("id") Long id,
                                 @Param("status") PaymentStatus status,
                                 @Param("response") String gatewayResponse);
}
