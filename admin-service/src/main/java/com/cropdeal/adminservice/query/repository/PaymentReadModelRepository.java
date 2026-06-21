package com.cropdeal.adminservice.query.repository;

import com.cropdeal.adminservice.query.model.PaymentReadModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentReadModelRepository extends JpaRepository<PaymentReadModel, Long> {

    Optional<PaymentReadModel> findByPaymentId(Long paymentId);

    boolean existsByPaymentId(Long paymentId);

    @Query("SELECT p FROM PaymentReadModel p WHERE " +
           "(:status   IS NULL OR p.paymentStatus = :status) AND " +
           "(:dealerId IS NULL OR p.dealerId      = :dealerId) AND " +
           "(:farmerId IS NULL OR p.farmerId      = :farmerId) AND " +
           "(:dateFrom IS NULL OR p.paidAt       >= :dateFrom) AND " +
           "(:dateTo   IS NULL OR p.paidAt       <= :dateTo)")
    Page<PaymentReadModel> findByFilters(
            @Param("status")   String status,
            @Param("dealerId") Long dealerId,
            @Param("farmerId") Long farmerId,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo")   LocalDateTime dateTo,
            Pageable pageable);

    @Query("SELECT p FROM PaymentReadModel p WHERE " +
           "(:status   IS NULL OR p.paymentStatus = :status) AND " +
           "(:dealerId IS NULL OR p.dealerId      = :dealerId) AND " +
           "(:farmerId IS NULL OR p.farmerId      = :farmerId) AND " +
           "(:dateFrom IS NULL OR p.paidAt       >= :dateFrom) AND " +
           "(:dateTo   IS NULL OR p.paidAt       <= :dateTo) " +
           "ORDER BY p.paidAt DESC")
    List<PaymentReadModel> findAllByFilters(
            @Param("status")   String status,
            @Param("dealerId") Long dealerId,
            @Param("farmerId") Long farmerId,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo")   LocalDateTime dateTo);

    @Query("SELECT SUM(p.amount) FROM PaymentReadModel p WHERE p.paymentStatus = 'SUCCESS' " +
           "AND (:dateFrom IS NULL OR p.paidAt >= :dateFrom) " +
           "AND (:dateTo   IS NULL OR p.paidAt <= :dateTo)")
    Double totalRevenue(@Param("dateFrom") LocalDateTime dateFrom,
                        @Param("dateTo")   LocalDateTime dateTo);

    @Query("SELECT COUNT(p) FROM PaymentReadModel p WHERE p.paymentStatus = :status")
    long countByStatus(@Param("status") String status);
}
