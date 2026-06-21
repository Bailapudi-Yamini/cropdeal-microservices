package com.cropdeal.orderservice.repository;

import com.cropdeal.orderservice.entity.Negotiation;
import com.cropdeal.orderservice.entity.NegotiationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NegotiationRepository extends JpaRepository<Negotiation, Long> {

    List<Negotiation> findByOrderIdOrderByCreatedAtAsc(Long orderId);

    /** Latest pending round for an order — only one PENDING round should exist at a time */
    Optional<Negotiation> findTopByOrderIdAndStatusOrderByCreatedAtDesc(Long orderId,
                                                                         NegotiationStatus status);

    @Modifying
    @Query("UPDATE Negotiation n SET n.status = :status WHERE n.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") NegotiationStatus status);

    /** Close all PENDING rounds for an order (used on cancel/confirm) */
    @Modifying
    @Query("UPDATE Negotiation n SET n.status = :status WHERE n.order.id = :orderId AND n.status = 'PENDING'")
    int closeAllPendingForOrder(@Param("orderId") Long orderId, @Param("status") NegotiationStatus status);

    long countByOrderId(Long orderId);
}
