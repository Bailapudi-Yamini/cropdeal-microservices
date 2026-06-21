package com.cropdeal.notificationservice.consumer;

import com.cropdeal.notificationservice.config.RabbitMQConfig;
import com.cropdeal.notificationservice.entity.NotificationType;
import com.cropdeal.notificationservice.event.PaymentSuccessEvent;
import com.cropdeal.notificationservice.service.EmailService;
import com.cropdeal.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSuccessConsumer {

    private final NotificationService notificationService;
    private final EmailService emailService;

    /**
     * Consumes payment.success events.
     * Sends receipt notifications to BOTH farmer and dealer.
     */
    @RabbitListener(queues = RabbitMQConfig.NOTIF_PAYMENT_SUCCESS_QUEUE)
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        log.info("Received payment.success: paymentId={}, orderId={}, receipt={}",
                event.getPaymentId(), event.getOrderId(), event.getReceiptNumber());

        // ── Notify farmer ─────────────────────────────────────────────────────
        String farmerTitle   = "Payment Received — Receipt " + event.getReceiptNumber();
        String farmerMessage = String.format(
                "Payment of ₹%.2f has been received for Order #%d. " +
                "Transaction ID: %s. Receipt: %s.",
                event.getAmount(), event.getOrderId(),
                event.getTransactionId(), event.getReceiptNumber());

        notificationService.createNotification(
                event.getFarmerId(), farmerTitle, farmerMessage,
                NotificationType.PAYMENT_SUCCESS, event.getPaymentId());

        emailService.sendEmail(
                resolveEmail(event.getFarmerId()),
                "[CropDeal] " + farmerTitle,
                farmerMessage + "\n\nView receipt: /payments/" + event.getPaymentId() + "/receipt");

        // ── Notify dealer ─────────────────────────────────────────────────────
        String dealerTitle   = "Payment Confirmed — Receipt " + event.getReceiptNumber();
        String dealerMessage = String.format(
                "Your payment of ₹%.2f for Order #%d has been confirmed. " +
                "Transaction ID: %s. Receipt: %s.",
                event.getAmount(), event.getOrderId(),
                event.getTransactionId(), event.getReceiptNumber());

        notificationService.createNotification(
                event.getDealerId(), dealerTitle, dealerMessage,
                NotificationType.PAYMENT_SUCCESS, event.getPaymentId());

        emailService.sendEmail(
                resolveEmail(event.getDealerId()),
                "[CropDeal] " + dealerTitle,
                dealerMessage + "\n\nView receipt: /payments/" + event.getPaymentId() + "/receipt");
    }

    private String resolveEmail(Long userId) {
        return "user-" + userId + "@cropdeal.com";
    }
}
