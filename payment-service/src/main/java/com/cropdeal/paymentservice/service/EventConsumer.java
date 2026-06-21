package com.cropdeal.paymentservice.service;

import com.cropdeal.paymentservice.config.RabbitMQConfig;
import com.cropdeal.paymentservice.event.OrderConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumer {

    private final PaymentService paymentService;
    private final EventSourcingService eventSourcingService;

    /**
     * Consumes order.confirmed from order-service.
     * Auto-initiates payment for the confirmed order.
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_CONFIRMED_QUEUE)
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Received order.confirmed for orderId={}", event.getOrderId());
        try {
            paymentService.handleOrderConfirmed(
                    event.getOrderId(),
                    event.getFarmerId(),
                    event.getDealerId(),
                    event.getCropListingId(),
                    event.getTotalAmount());
        } catch (Exception e) {
            log.error("Failed to handle order.confirmed for orderId={}: {}",
                    event.getOrderId(), e.getMessage(), e);
            // Message will be routed to DLQ by RabbitMQ after max retries
            throw e;
        }
    }

    /**
     * Self-consumed internal event — records PAYMENT_INITIATED in the event store.
     * Decouples event sourcing persistence from the main payment transaction.
     */
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_INITIATED_QUEUE)
    public void onPaymentInitiated(Map<String, Object> payload) {
        log.debug("Received payment.initiated internal event: paymentId={}",
                payload.get("paymentId"));
        // Event already appended synchronously in PaymentServiceImpl.initiatePayment()
        // This listener exists for observability and potential replay scenarios
    }
}
