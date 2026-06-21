package com.cropdeal.orderservice.service;

import com.cropdeal.orderservice.config.RabbitMQConfig;
import com.cropdeal.orderservice.event.NegotiationUpdateEvent;
import com.cropdeal.orderservice.event.OrderConfirmedEvent;
import com.cropdeal.orderservice.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderPlaced(OrderPlacedEvent event) {
        log.info("Publishing order.placed for orderId={}", event.getOrderId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ORDER_PLACED_KEY, event);
    }

    public void publishNegotiationUpdate(NegotiationUpdateEvent event) {
        log.info("Publishing negotiation.update for orderId={}, status={}",
                event.getOrderId(), event.getNegotiationStatus());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.NEGOTIATION_UPDATE_KEY, event);
    }

    public void publishOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Publishing order.confirmed for orderId={}", event.getOrderId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ORDER_CONFIRMED_KEY, event);
    }
}
