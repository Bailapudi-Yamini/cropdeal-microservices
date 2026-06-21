package com.cropdeal.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE     = "cropdeal.exchange";
    public static final String DLQ_EXCHANGE = "cropdeal.dlq.exchange";

    // ── Routing keys (must match publisher routing keys exactly) ─────────────
    public static final String CROP_POSTED_KEY        = "crop.posted";
    public static final String ORDER_PLACED_KEY       = "order.placed";
    public static final String NEGOTIATION_UPDATE_KEY = "negotiation.update";
    public static final String PAYMENT_SUCCESS_KEY    = "payment.success";
    public static final String PAYMENT_FAILED_KEY     = "payment.failed";

    // ── Notification-service owned queues ─────────────────────────────────────
    // Each queue has a unique name so it gets its own copy of every message
    // (RabbitMQ topic exchange fan-out per binding)
    public static final String NOTIF_CROP_POSTED_QUEUE        = "notif.crop.posted.queue";
    public static final String NOTIF_ORDER_PLACED_QUEUE       = "notif.order.placed.queue";
    public static final String NOTIF_NEGOTIATION_UPDATE_QUEUE = "notif.negotiation.update.queue";
    public static final String NOTIF_PAYMENT_SUCCESS_QUEUE    = "notif.payment.success.queue";
    public static final String NOTIF_PAYMENT_FAILED_QUEUE     = "notif.payment.failed.queue";

    // ── Dead-letter queue ─────────────────────────────────────────────────────
    public static final String NOTIF_DLQ = "notif.dlq";

    // ── Exchange ──────────────────────────────────────────────────────────────
    @Bean
    public TopicExchange cropDealExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange dlqExchange() {
        return new TopicExchange(DLQ_EXCHANGE, true, false);
    }

    // ── Dead-letter queue ─────────────────────────────────────────────────────
    @Bean
    public Queue notifDlq() {
        return QueueBuilder.durable(NOTIF_DLQ).build();
    }

    @Bean
    public Binding notifDlqBinding() {
        return BindingBuilder.bind(notifDlq()).to(dlqExchange()).with(NOTIF_DLQ);
    }

    // ── crop.posted ───────────────────────────────────────────────────────────
    @Bean
    public Queue notifCropPostedQueue() {
        return QueueBuilder.durable(NOTIF_CROP_POSTED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", NOTIF_DLQ)
                .build();
    }

    @Bean
    public Binding notifCropPostedBinding() {
        return BindingBuilder.bind(notifCropPostedQueue()).to(cropDealExchange()).with(CROP_POSTED_KEY);
    }

    // ── order.placed ──────────────────────────────────────────────────────────
    @Bean
    public Queue notifOrderPlacedQueue() {
        return QueueBuilder.durable(NOTIF_ORDER_PLACED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", NOTIF_DLQ)
                .build();
    }

    @Bean
    public Binding notifOrderPlacedBinding() {
        return BindingBuilder.bind(notifOrderPlacedQueue()).to(cropDealExchange()).with(ORDER_PLACED_KEY);
    }

    // ── negotiation.update ────────────────────────────────────────────────────
    @Bean
    public Queue notifNegotiationUpdateQueue() {
        return QueueBuilder.durable(NOTIF_NEGOTIATION_UPDATE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", NOTIF_DLQ)
                .build();
    }

    @Bean
    public Binding notifNegotiationUpdateBinding() {
        return BindingBuilder.bind(notifNegotiationUpdateQueue())
                .to(cropDealExchange()).with(NEGOTIATION_UPDATE_KEY);
    }

    // ── payment.success ───────────────────────────────────────────────────────
    @Bean
    public Queue notifPaymentSuccessQueue() {
        return QueueBuilder.durable(NOTIF_PAYMENT_SUCCESS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", NOTIF_DLQ)
                .build();
    }

    @Bean
    public Binding notifPaymentSuccessBinding() {
        return BindingBuilder.bind(notifPaymentSuccessQueue())
                .to(cropDealExchange()).with(PAYMENT_SUCCESS_KEY);
    }

    // ── payment.failed ────────────────────────────────────────────────────────
    @Bean
    public Queue notifPaymentFailedQueue() {
        return QueueBuilder.durable(NOTIF_PAYMENT_FAILED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", NOTIF_DLQ)
                .build();
    }

    @Bean
    public Binding notifPaymentFailedBinding() {
        return BindingBuilder.bind(notifPaymentFailedQueue())
                .to(cropDealExchange()).with(PAYMENT_FAILED_KEY);
    }

    // ── Converter + Template ──────────────────────────────────────────────────
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

    /**
     * Retry template: 3 attempts with exponential back-off (2s, 4s, 8s).
     * After exhaustion, RepublishMessageRecoverer routes to DLQ.
     */
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retry = new RetryTemplate();
        SimpleRetryPolicy policy = new SimpleRetryPolicy();
        policy.setMaxAttempts(3);
        retry.setRetryPolicy(policy);
        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(2000);
        backOff.setMultiplier(2.0);
        retry.setBackOffPolicy(backOff);
        return retry;
    }

    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, DLQ_EXCHANGE, NOTIF_DLQ);
    }
}
