package com.cropdeal.orderservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "cropdeal.exchange";

    // Published by order-service
    public static final String ORDER_PLACED_KEY        = "order.placed";
    public static final String NEGOTIATION_UPDATE_KEY  = "negotiation.update";
    public static final String ORDER_CONFIRMED_KEY     = "order.confirmed";

    // Queues declared by order-service for its own consumers
    public static final String ORDER_PLACED_QUEUE       = "order.placed.queue";
    public static final String NEGOTIATION_UPDATE_QUEUE = "negotiation.update.queue";

    // Consumed by order-service: payment-service publishes payment.success
    public static final String PAYMENT_SUCCESS_QUEUE = "order.payment.success.queue";
    public static final String PAYMENT_SUCCESS_KEY   = "payment.success";

    @Bean
    public TopicExchange cropDealExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    // --- order.placed ---
    @Bean
    public Queue orderPlacedQueue() {
        return QueueBuilder.durable(ORDER_PLACED_QUEUE).build();
    }

    @Bean
    public Binding orderPlacedBinding() {
        return BindingBuilder.bind(orderPlacedQueue()).to(cropDealExchange()).with(ORDER_PLACED_KEY);
    }

    // --- negotiation.update ---
    @Bean
    public Queue negotiationUpdateQueue() {
        return QueueBuilder.durable(NEGOTIATION_UPDATE_QUEUE).build();
    }

    @Bean
    public Binding negotiationUpdateBinding() {
        return BindingBuilder.bind(negotiationUpdateQueue()).to(cropDealExchange()).with(NEGOTIATION_UPDATE_KEY);
    }

    // --- payment.success (consumed to mark order COMPLETED) ---
    @Bean
    public Queue orderPaymentSuccessQueue() {
        return QueueBuilder.durable(PAYMENT_SUCCESS_QUEUE).build();
    }

    @Bean
    public Binding orderPaymentSuccessBinding() {
        return BindingBuilder.bind(orderPaymentSuccessQueue()).to(cropDealExchange()).with(PAYMENT_SUCCESS_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
