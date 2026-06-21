package com.cropdeal.notificationservice.consumer;

import com.cropdeal.notificationservice.config.RabbitMQConfig;
import com.cropdeal.notificationservice.entity.NotificationType;
import com.cropdeal.notificationservice.event.PaymentFailedEvent;
import com.cropdeal.notificationservice.service.EmailService;
import com.cropdeal.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFailedConsumer {

    private final NotificationService notificationService;
    private final EmailService emailService;

    /**
     * Consumes payment.failed events.
     * Alerts the dealer to retry the payment.
     */
    @RabbitListener(queues = RabbitMQConfig.NOTIF_PAYMENT_FAILED_QUEUE)
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.info("Received payment.failed: paymentId={}, orderId={}, reason={}",
                event.getPaymentId(), event.getOrderId(), event.getFailureReason());

        String title = "Payment Failed — Order #" + event.getOrderId();
        String message = String.format(
                "Your payment of ₹%.2f for Order #%d could not be processed. " +
                "Reason: %s. Transaction ID: %s. Please retry.",
                event.getAmount(), event.getOrderId(),
                event.getFailureReason(), event.getTransactionId());

        // Notify dealer to retry
        notificationService.createNotification(
                event.getDealerId(), title, message,
                NotificationType.PAYMENT_FAILED, event.getPaymentId());

        emailService.sendEmail(
                resolveEmail(event.getDealerId()),
                "[CropDeal] ACTION REQUIRED: " + title,
                message + "\n\nRetry payment: /payments/order/" + event.getOrderId());
    }

    private String resolveEmail(Long userId) {
        return "user-" + userId + "@cropdeal.com";
    }
}
