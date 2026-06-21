package com.cropdeal.cropservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Shared exchange — all CropDeal services use the same topic exchange
    public static final String EXCHANGE = "cropdeal.exchange";

    // crop-service publishes this
    public static final String CROP_POSTED_QUEUE = "crop.posted.queue";
    public static final String CROP_POSTED_KEY   = "crop.posted";

    // crop-service consumes this (payment-service publishes payment.success
    // so order-service and crop-service can mark listing as SOLD)
    public static final String PAYMENT_SUCCESS_QUEUE = "crop.payment.success.queue";
    public static final String PAYMENT_SUCCESS_KEY   = "payment.success";

    @Bean
    public TopicExchange cropDealExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    // --- crop.posted ---
    @Bean
    public Queue cropPostedQueue() {
        return QueueBuilder.durable(CROP_POSTED_QUEUE).build();
    }

    @Bean
    public Binding cropPostedBinding() {
        return BindingBuilder.bind(cropPostedQueue()).to(cropDealExchange()).with(CROP_POSTED_KEY);
    }

    // --- payment.success (consumed to mark listing SOLD) ---
    @Bean
    public Queue cropPaymentSuccessQueue() {
        return QueueBuilder.durable(PAYMENT_SUCCESS_QUEUE).build();
    }

    @Bean
    public Binding cropPaymentSuccessBinding() {
        return BindingBuilder.bind(cropPaymentSuccessQueue()).to(cropDealExchange()).with(PAYMENT_SUCCESS_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
