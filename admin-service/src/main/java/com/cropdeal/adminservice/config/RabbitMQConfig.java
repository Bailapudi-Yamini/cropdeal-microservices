package com.cropdeal.adminservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE     = "cropdeal.exchange";
    public static final String DLQ_EXCHANGE = "cropdeal.dlq.exchange";

    // Routing keys
    public static final String ORDER_PLACED_KEY    = "order.placed";
    public static final String PAYMENT_SUCCESS_KEY = "payment.success";
    public static final String PAYMENT_FAILED_KEY  = "payment.failed";

    // Admin-service owned queues — unique names for independent fan-out copies
    public static final String ADMIN_ORDER_PLACED_QUEUE    = "admin.order.placed.queue";
    public static final String ADMIN_PAYMENT_SUCCESS_QUEUE = "admin.payment.success.queue";
    public static final String ADMIN_PAYMENT_FAILED_QUEUE  = "admin.payment.failed.queue";
    public static final String ADMIN_DLQ                   = "admin.dlq";

    @Bean public TopicExchange cropDealExchange() { return new TopicExchange(EXCHANGE, true, false); }
    @Bean public TopicExchange dlqExchange()      { return new TopicExchange(DLQ_EXCHANGE, true, false); }

    @Bean
    public Queue adminDlq() { return QueueBuilder.durable(ADMIN_DLQ).build(); }

    @Bean
    public Binding adminDlqBinding() {
        return BindingBuilder.bind(adminDlq()).to(dlqExchange()).with(ADMIN_DLQ);
    }

    // ── order.placed ──────────────────────────────────────────────────────────
    @Bean
    public Queue adminOrderPlacedQueue() {
        return QueueBuilder.durable(ADMIN_ORDER_PLACED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ADMIN_DLQ).build();
    }

    @Bean
    public Binding adminOrderPlacedBinding() {
        return BindingBuilder.bind(adminOrderPlacedQueue()).to(cropDealExchange()).with(ORDER_PLACED_KEY);
    }

    // ── payment.success ───────────────────────────────────────────────────────
    @Bean
    public Queue adminPaymentSuccessQueue() {
        return QueueBuilder.durable(ADMIN_PAYMENT_SUCCESS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ADMIN_DLQ).build();
    }

    @Bean
    public Binding adminPaymentSuccessBinding() {
        return BindingBuilder.bind(adminPaymentSuccessQueue()).to(cropDealExchange()).with(PAYMENT_SUCCESS_KEY);
    }

    // ── payment.failed ────────────────────────────────────────────────────────
    @Bean
    public Queue adminPaymentFailedQueue() {
        return QueueBuilder.durable(ADMIN_PAYMENT_FAILED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ADMIN_DLQ).build();
    }

    @Bean
    public Binding adminPaymentFailedBinding() {
        return BindingBuilder.bind(adminPaymentFailedQueue()).to(cropDealExchange()).with(PAYMENT_FAILED_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate t = new RabbitTemplate(connectionFactory);
        t.setMessageConverter(messageConverter());
        return t;
    }

    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, DLQ_EXCHANGE, ADMIN_DLQ);
    }
}
