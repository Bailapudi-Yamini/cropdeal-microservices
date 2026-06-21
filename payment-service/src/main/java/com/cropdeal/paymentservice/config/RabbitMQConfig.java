package com.cropdeal.paymentservice.config;

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

    public static final String EXCHANGE     = "cropdeal.exchange";
    public static final String DLQ_EXCHANGE = "cropdeal.dlq.exchange";

    // ── Keys published by payment-service ────────────────────────────────────
    public static final String PAYMENT_SUCCESS_KEY        = "payment.success";
    public static final String PAYMENT_FAILED_KEY         = "payment.failed";
    public static final String PAYMENT_CHECKOUT_READY_KEY = "payment.checkout.ready";

    // ── Queue for checkout.ready (consumed by notification-service / frontend poll) ──
    public static final String PAYMENT_CHECKOUT_READY_QUEUE = "payment.checkout.ready.queue";

    // ── Queues owned by payment-service ──────────────────────────────────────

    // Consumed: order.confirmed from order-service → triggers payment initiation
    public static final String ORDER_CONFIRMED_QUEUE = "payment.order.confirmed.queue";
    public static final String ORDER_CONFIRMED_KEY   = "order.confirmed";

    // Published + self-consumed for Event Sourcing internal processing
    public static final String PAYMENT_INITIATED_QUEUE = "payment.initiated.queue";
    public static final String PAYMENT_INITIATED_KEY   = "payment.initiated";

    // Outbound — consumed by order-service, crop-service, notification-service
    public static final String PAYMENT_SUCCESS_QUEUE = "payment.success.queue";
    public static final String PAYMENT_FAILED_QUEUE  = "payment.failed.queue";

    // Dead-letter queue for failed message processing
    public static final String PAYMENT_DLQ = "payment.dlq";

    @Bean
    public TopicExchange cropDealExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange dlqExchange() {
        return new TopicExchange(DLQ_EXCHANGE, true, false);
    }

    // ── order.confirmed (inbound) ─────────────────────────────────────────────
    @Bean
    public Queue orderConfirmedQueue() {
        return QueueBuilder.durable(ORDER_CONFIRMED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", PAYMENT_DLQ)
                .build();
    }

    @Bean
    public Binding orderConfirmedBinding() {
        return BindingBuilder.bind(orderConfirmedQueue()).to(cropDealExchange()).with(ORDER_CONFIRMED_KEY);
    }

    // ── payment.initiated (internal event sourcing) ───────────────────────────
    @Bean
    public Queue paymentInitiatedQueue() {
        return QueueBuilder.durable(PAYMENT_INITIATED_QUEUE).build();
    }

    @Bean
    public Binding paymentInitiatedBinding() {
        return BindingBuilder.bind(paymentInitiatedQueue()).to(cropDealExchange()).with(PAYMENT_INITIATED_KEY);
    }

    // ── payment.checkout.ready (outbound) ────────────────────────────────────
    @Bean
    public Queue paymentCheckoutReadyQueue() {
        return QueueBuilder.durable(PAYMENT_CHECKOUT_READY_QUEUE).build();
    }

    @Bean
    public Binding paymentCheckoutReadyBinding() {
        return BindingBuilder.bind(paymentCheckoutReadyQueue()).to(cropDealExchange()).with(PAYMENT_CHECKOUT_READY_KEY);
    }

    // ── payment.success (outbound) ────────────────────────────────────────────
    @Bean
    public Queue paymentSuccessQueue() {
        return QueueBuilder.durable(PAYMENT_SUCCESS_QUEUE).build();
    }

    @Bean
    public Binding paymentSuccessBinding() {
        return BindingBuilder.bind(paymentSuccessQueue()).to(cropDealExchange()).with(PAYMENT_SUCCESS_KEY);
    }

    // ── payment.failed (outbound) ─────────────────────────────────────────────
    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(PAYMENT_FAILED_QUEUE).build();
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentFailedQueue()).to(cropDealExchange()).with(PAYMENT_FAILED_KEY);
    }

    // ── Dead-letter queue ─────────────────────────────────────────────────────
    @Bean
    public Queue paymentDlq() {
        return QueueBuilder.durable(PAYMENT_DLQ).build();
    }

    @Bean
    public Binding paymentDlqBinding() {
        return BindingBuilder.bind(paymentDlq()).to(dlqExchange()).with(PAYMENT_DLQ);
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
