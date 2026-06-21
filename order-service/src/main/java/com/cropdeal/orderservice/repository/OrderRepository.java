package com.cropdeal.orderservice.repository;

import com.cropdeal.orderservice.entity.Order;
import com.cropdeal.orderservice.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByDealerId(Long dealerId, Pageable pageable);

    Page<Order> findByFarmerId(Long farmerId, Pageable pageable);

    Page<Order> findByDealerIdAndStatus(Long dealerId, OrderStatus status, Pageable pageable);

    Page<Order> findByFarmerIdAndStatus(Long farmerId, OrderStatus status, Pageable pageable);

    Optional<Order> findByIdAndDealerId(Long id, Long dealerId);

    Optional<Order> findByIdAndFarmerId(Long id, Long farmerId);

    /** Used by payment-service consumer to locate the order by listing */
    Optional<Order> findByCropListingIdAndStatus(Long cropListingId, OrderStatus status);

    @Modifying
    @Query("UPDATE Order o SET o.status = :status WHERE o.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.farmerId = :farmerId AND o.status = :status")
    long countByFarmerIdAndStatus(@Param("farmerId") Long farmerId, @Param("status") OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.dealerId = :dealerId AND o.status = :status")
    long countByDealerIdAndStatus(@Param("dealerId") Long dealerId, @Param("status") OrderStatus status);
}
