package com.cropdeal.paymentservice.repository;

import com.cropdeal.paymentservice.entity.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {

    /** Returns the full event stream for a payment in chronological order — used for replay */
    List<PaymentEvent> findByPaymentIdOrderByOccurredAtAsc(Long paymentId);

    /** Latest event for a payment — used for current-state projection */
    PaymentEvent findTopByPaymentIdOrderByOccurredAtDesc(Long paymentId);

    long countByPaymentId(Long paymentId);
}
