package com.cropdeal.notificationservice.consumer;

import com.cropdeal.notificationservice.config.RabbitMQConfig;
import com.cropdeal.notificationservice.entity.NotificationType;
import com.cropdeal.notificationservice.event.CropPostedEvent;
import com.cropdeal.notificationservice.service.EmailService;
import com.cropdeal.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CropPostedConsumer {

    private final NotificationService notificationService;
    private final EmailService emailService;

    /**
     * Consumes crop.posted events.
     * Fans out one in-app notification per subscribed dealer.
     * subscribedDealerIds are pre-resolved by crop-service at publish time.
     */
    @RabbitListener(queues = RabbitMQConfig.NOTIF_CROP_POSTED_QUEUE)
    public void onCropPosted(CropPostedEvent event) {
        log.info("Received crop.posted: listingId={}, cropType={}, dealers={}",
                event.getListingId(), event.getCropType(),
                event.getSubscribedDealerIds() != null ? event.getSubscribedDealerIds().size() : 0);

        if (event.getSubscribedDealerIds() == null || event.getSubscribedDealerIds().isEmpty()) {
            log.debug("No subscribed dealers for listingId={}", event.getListingId());
            return;
        }

        String title = "New Crop Available: " + event.getCropName();
        String message = String.format(
                "%s has listed %s %.2f %s at ₹%.2f/unit in %s.",
                event.getFarmerName() != null ? event.getFarmerName() : "A farmer",
                event.getCropName(),
                event.getQuantityAvailable(),
                event.getUnit(),
                event.getPricePerUnit(),
                event.getLocation());

        // Fan-out: one notification per subscribed dealer
        for (Long dealerId : event.getSubscribedDealerIds()) {
            notificationService.createNotification(
                    dealerId, title, message,
                    NotificationType.CROP_POSTED, event.getListingId());

            // Email is async and non-blocking — failure won't affect in-app notification
            emailService.sendEmail(
                    resolveEmail(dealerId),
                    "[CropDeal] " + title,
                    message + "\n\nView listing: /crops/" + event.getListingId());
        }
    }

    /**
     * In production: call user-service via Feign to get the dealer's email.
     * Kept as a stub here to avoid circular Feign dependency in this layer.
     */
    private String resolveEmail(Long userId) {
        return "user-" + userId + "@cropdeal.com";
    }
}
