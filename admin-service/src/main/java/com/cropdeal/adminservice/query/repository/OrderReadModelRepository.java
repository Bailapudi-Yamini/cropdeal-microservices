package com.cropdeal.adminservice.query.repository;

import com.cropdeal.adminservice.query.model.OrderReadModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderReadModelRepository extends JpaRepository<OrderReadModel, Long> {

    Optional<OrderReadModel> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    Page<OrderReadModel> findByDealerId(Long dealerId, Pageable pageable);

    Page<OrderReadModel> findByFarmerId(Long farmerId, Pageable pageable);

    /** Core report query — all filters optional */
    @Query("SELECT o FROM OrderReadModel o WHERE " +
           "(:status    IS NULL OR o.orderStatus = :status) AND " +
           "(:cropType  IS NULL OR o.cropType    = :cropType) AND " +
           "(:dealerId  IS NULL OR o.dealerId    = :dealerId) AND " +
           "(:farmerId  IS NULL OR o.farmerId    = :farmerId) AND " +
           "(:dateFrom  IS NULL OR o.placedAt   >= :dateFrom) AND " +
           "(:dateTo    IS NULL OR o.placedAt   <= :dateTo)")
    Page<OrderReadModel> findByFilters(
            @Param("status")   String status,
            @Param("cropType") String cropType,
            @Param("dealerId") Long dealerId,
            @Param("farmerId") Long farmerId,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo")   LocalDateTime dateTo,
            Pageable pageable);

    /** Same as above but returns full list — used for Excel export */
    @Query("SELECT o FROM OrderReadModel o WHERE " +
           "(:status    IS NULL OR o.orderStatus = :status) AND " +
           "(:cropType  IS NULL OR o.cropType    = :cropType) AND " +
           "(:dealerId  IS NULL OR o.dealerId    = :dealerId) AND " +
           "(:farmerId  IS NULL OR o.farmerId    = :farmerId) AND " +
           "(:dateFrom  IS NULL OR o.placedAt   >= :dateFrom) AND " +
           "(:dateTo    IS NULL OR o.placedAt   <= :dateTo) " +
           "ORDER BY o.placedAt DESC")
    List<OrderReadModel> findAllByFilters(
            @Param("status")   String status,
            @Param("cropType") String cropType,
            @Param("dealerId") Long dealerId,
            @Param("farmerId") Long farmerId,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo")   LocalDateTime dateTo);

    /** Revenue summary per dealer — used for dealer performance report */
    @Query("SELECT o.dealerId, SUM(o.totalAmount), COUNT(o) " +
           "FROM OrderReadModel o WHERE o.orderStatus = 'COMPLETED' " +
           "AND (:dateFrom IS NULL OR o.placedAt >= :dateFrom) " +
           "AND (:dateTo   IS NULL OR o.placedAt <= :dateTo) " +
           "GROUP BY o.dealerId ORDER BY SUM(o.totalAmount) DESC")
    List<Object[]> dealerRevenueSummary(
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo")   LocalDateTime dateTo);

    @Query("SELECT COUNT(o) FROM OrderReadModel o WHERE o.orderStatus = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT SUM(o.totalAmount) FROM OrderReadModel o WHERE o.orderStatus = 'COMPLETED'")
    Double totalRevenue();
}
