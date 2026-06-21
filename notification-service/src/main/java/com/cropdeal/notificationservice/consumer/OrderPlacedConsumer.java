package com.cropdeal.notificationservice.consumer;

import com.cropdeal.notificationservice.config.RabbitMQConfig;
import com.cropdeal.notificationservice.entity.NotificationType;
import com.cropdeal.notificationservice.event.OrderPlacedEvent;
import com.cropdeal.notificationservice.service.EmailService;
import com.cropdeal.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPlacedConsumer {

    private final NotificationService notificationService;
    private final EmailService emailService;

    /**
     * Consumes order.placed events.
     * Notifies the farmer that a dealer has placed an order on their listing.
     */
    @RabbitListener(queues = RabbitMQConfig.NOTIF_ORDER_PLACED_QUEUE)
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Received order.placed: orderId={}, farmerId={}, dealerId={}",
                event.getOrderId(), event.getFarmerId(), event.getDealerId());

        String title = "New Order Received — Order #" + event.getOrderId();
        String message = String.format(
                "A dealer has placed an order for %.2f units at ₹%.2f/unit. " +
                "Total: ₹%.2f. Notes: %s",
                event.getQuantity(),
                event.getAgreedPricePerUnit(),
                event.getTotalAmount(),
                event.getDealerNotes() != null ? event.getDealerNotes() : "None");

        // Notify farmer
        notificationService.createNotification(
                event.getFarmerId(), title, message,
                NotificationType.ORDER_PLACED, event.getOrderId());

        emailService.sendEmail(
                resolveEmail(event.getFarmerId()),
                "[CropDeal] " + title,
                message + "\n\nView order: /orders/" + event.getOrderId());
    }

    private String resolveEmail(Long userId) {
        return "user-" + userId + "@cropdeal.com";
    }
}
