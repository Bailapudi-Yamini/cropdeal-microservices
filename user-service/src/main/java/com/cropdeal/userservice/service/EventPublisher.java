package com.cropdeal.userservice.service;

import com.cropdeal.userservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishUserRegistered(Object payload) {
        log.info("Publishing user.registered event: {}", payload);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.USER_REGISTERED_KEY, payload);
    }
}
