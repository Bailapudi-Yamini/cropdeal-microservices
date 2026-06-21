package com.cropdeal.notificationservice.consumer;

import com.cropdeal.notificationservice.config.RabbitMQConfig;
import com.cropdeal.notificationservice.entity.NotificationType;
import com.cropdeal.notificationservice.event.NegotiationUpdateEvent;
import com.cropdeal.notificationservice.service.EmailService;
import com.cropdeal.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NegotiationUpdateConsumer {

    private final NotificationService notificationService;
    private final EmailService emailService;

    /**
     * Consumes negotiation.update events.
     * Notifies the respondingParty (pre-resolved by order-service) with a
     * context-aware message based on the negotiation status.
     */
    @RabbitListener(queues = RabbitMQConfig.NOTIF_NEGOTIATION_UPDATE_QUEUE)
    public void onNegotiationUpdate(NegotiationUpdateEvent event) {
        log.info("Received negotiation.update: orderId={}, status={}, respondingParty={}",
                event.getOrderId(), event.getNegotiationStatus(), event.getRespondingParty());

        String title   = buildTitle(event);
        String message = buildMessage(event);

        // Notify the responding party — the one who needs to act next
        notificationService.createNotification(
                event.getRespondingParty(), title, message,
                NotificationType.NEGOTIATION_UPDATE, event.getOrderId());

        emailService.sendEmail(
                resolveEmail(event.getRespondingParty()),
                "[CropDeal] " + title,
                message + "\n\nView order: /orders/" + event.getOrderId());
    }

    private String buildTitle(NegotiationUpdateEvent event) {
        return switch (event.getNegotiationStatus()) {
            case "PENDING"    -> "Price Proposal — Order #" + event.getOrderId()
                                 + " (Round " + event.getRoundNumber() + ")";
            case "ACCEPTED"   -> "Negotiation Accepted — Order #" + event.getOrderId() + " Confirmed!";
            case "REJECTED"   -> "Negotiation Rejected — Order #" + event.getOrderId() + " Cancelled";
            case "COUNTERED"  -> "Counter Offer — Order #" + event.getOrderId();
            default           -> "Negotiation Update — Order #" + event.getOrderId();
        };
    }

    private String buildMessage(NegotiationUpdateEvent event) {
        return switch (event.getNegotiationStatus()) {
            case "PENDING" -> String.format(
                    "A price of ₹%.2f/unit has been proposed for Order #%d. " +
                    "Please accept, reject, or counter.",
                    event.getProposedPrice(), event.getOrderId());
            case "ACCEPTED" -> String.format(
                    "The proposed price of ₹%.2f/unit has been accepted. " +
                    "Order #%d is now CONFIRMED. Payment will be processed shortly.",
                    event.getProposedPrice(), event.getOrderId());
            case "REJECTED" -> String.format(
                    "The proposed price of ₹%.2f/unit was rejected. " +
                    "Order #%d has been CANCELLED.",
                    event.getProposedPrice(), event.getOrderId());
            case "COUNTERED" -> String.format(
                    "A counter offer of ₹%.2f/unit has been made for Order #%d. " +
                    "Message: %s",
                    event.getProposedPrice(), event.getOrderId(),
                    event.getMessage() != null ? event.getMessage() : "No message");
            default -> "Negotiation status updated for Order #" + event.getOrderId();
        };
    }

    private String resolveEmail(Long userId) {
        return "user-" + userId + "@cropdeal.com";
    }
}
