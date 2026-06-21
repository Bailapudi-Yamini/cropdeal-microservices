package com.cropdeal.cropservice.service;

import com.cropdeal.cropservice.config.RabbitMQConfig;
import com.cropdeal.cropservice.event.CropPostedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes crop.posted event to cropdeal.exchange.
     * notification-service consumes this to alert subscribed dealers.
     */
    public void publishCropPosted(CropPostedEvent event) {
        log.info("Publishing crop.posted event for listingId={}, cropType={}, subscribers={}",
                event.getListingId(), event.getCropType(), event.getSubscribedDealerIds().size());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.CROP_POSTED_KEY, event);
    }
}
