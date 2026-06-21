package com.cropdeal.cropservice.service;

import com.cropdeal.cropservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes payment.success events published by payment-service.
 * Updates the crop listing status to SOLD.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumer {

    private final CropListingService cropListingService;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_SUCCESS_QUEUE)
    public void onPaymentSuccess(Map<String, Object> payload) {
        try {
            Long listingId = Long.valueOf(payload.get("listingId").toString());
            log.info("Received payment.success for listingId={}", listingId);
            cropListingService.markListingAsSold(listingId);
        } catch (Exception e) {
            log.error("Failed to process payment.success event: {}", e.getMessage(), e);
            // In production: send to a dead-letter queue instead of swallowing
        }
    }
}
