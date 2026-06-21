package com.cropdeal.adminservice.consumer;

import com.cropdeal.adminservice.config.RabbitMQConfig;
import com.cropdeal.adminservice.event.OrderPlacedEvent;
import com.cropdeal.adminservice.event.PaymentFailedEvent;
import com.cropdeal.adminservice.event.PaymentSuccessEvent;
import com.cropdeal.adminservice.query.model.OrderReadModel;
import com.cropdeal.adminservice.query.model.PaymentReadModel;
import com.cropdeal.adminservice.query.repository.OrderReadModelRepository;
import com.cropdeal.adminservice.query.repository.PaymentReadModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * CQRS — populates the read-side models from domain events.
 * These are the only writers to OrderReadModel and PaymentReadModel.
 * No controller or service ever writes to these tables directly.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminEventConsumer {

    private final OrderReadModelRepository orderReadModelRepository;
    private final PaymentReadModelRepository paymentReadModelRepository;

    /**
     * order.placed → insert a new row into the order read model.
     * cropType and cropName are not in this event — they are enriched
     * when payment.success arrives and the row is updated.
     */
    @RabbitListener(queues = RabbitMQConfig.ADMIN_ORDER_PLACED_QUEUE)
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("CQRS: projecting order.placed orderId={}", event.getOrderId());

        if (orderReadModelRepository.existsByOrderId(event.getOrderId())) {
            log.warn("OrderReadModel already exists for orderId={}, skipping", event.getOrderId());
            return;
        }

        OrderReadModel projection = OrderReadModel.builder()
                .orderId(event.getOrderId())
                .cropListingId(event.getCropListingId())
                .farmerId(event.getFarmerId())
                .dealerId(event.getDealerId())
                .quantity(event.getQuantity())
                .agreedPricePerUnit(event.getAgreedPricePerUnit())
                .totalAmount(event.getTotalAmount())
                .orderStatus("PENDING")
                .placedAt(event.getPlacedAt())
                .build();

        orderReadModelRepository.save(projection);
    }

    /**
     * payment.success → insert payment read model row + update order status to COMPLETED.
     */
    @RabbitListener(queues = RabbitMQConfig.ADMIN_PAYMENT_SUCCESS_QUEUE)
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        log.info("CQRS: projecting payment.success paymentId={}, orderId={}",
                event.getPaymentId(), event.getOrderId());

        // Upsert payment read model
        if (!paymentReadModelRepository.existsByPaymentId(event.getPaymentId())) {
            paymentReadModelRepository.save(PaymentReadModel.builder()
                    .paymentId(event.getPaymentId())
                    .orderId(event.getOrderId())
                    .farmerId(event.getFarmerId())
                    .dealerId(event.getDealerId())
                    .amount(event.getAmount())
                    .paymentStatus("SUCCESS")
                    .transactionId(event.getTransactionId())
                    .receiptNumber(event.getReceiptNumber())
                    .paidAt(event.getPaidAt())
                    .build());
        }

        // Update order read model status to COMPLETED
        orderReadModelRepository.findByOrderId(event.getOrderId()).ifPresent(order -> {
            order.setOrderStatus("COMPLETED");
            order.setCompletedAt(event.getPaidAt());
            orderReadModelRepository.save(order);
        });
    }

    /**
     * payment.failed → insert payment read model row with FAILED status.
     */
    @RabbitListener(queues = RabbitMQConfig.ADMIN_PAYMENT_FAILED_QUEUE)
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.info("CQRS: projecting payment.failed paymentId={}", event.getPaymentId());

        if (paymentReadModelRepository.existsByPaymentId(event.getPaymentId())) {
            return;
        }

        paymentReadModelRepository.save(PaymentReadModel.builder()
                .paymentId(event.getPaymentId())
                .orderId(event.getOrderId())
                .farmerId(event.getFarmerId())
                .dealerId(event.getDealerId())
                .amount(event.getAmount())
                .paymentStatus("FAILED")
                .transactionId(event.getTransactionId())
                .paidAt(event.getFailedAt())
                .build());
    }
}
