package com.cropdeal.orderservice.service;

import com.cropdeal.orderservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes payment.success published by payment-service.
 * Marks the corresponding order as COMPLETED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumer {

    private final OrderService orderService;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_SUCCESS_QUEUE)
    public void onPaymentSuccess(Map<String, Object> payload) {
        try {
            Long orderId = Long.valueOf(payload.get("orderId").toString());
            log.info("Received payment.success for orderId={}", orderId);
            orderService.markOrderCompleted(orderId);
        } catch (Exception e) {
            log.error("Failed to process payment.success in order-service: {}", e.getMessage(), e);
        }
    }
}
