package com.cropdeal.paymentservice.service;

import com.cropdeal.paymentservice.config.RabbitMQConfig;
import com.cropdeal.paymentservice.event.PaymentCheckoutReadyEvent;
import com.cropdeal.paymentservice.event.PaymentFailedEvent;
import com.cropdeal.paymentservice.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishPaymentSuccess(PaymentSuccessEvent event) {
        log.info("Publishing payment.success for paymentId={}, orderId={}",
                event.getPaymentId(), event.getOrderId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.PAYMENT_SUCCESS_KEY, event);
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        log.info("Publishing payment.failed for paymentId={}, orderId={}",
                event.getPaymentId(), event.getOrderId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.PAYMENT_FAILED_KEY, event);
    }

    public void publishCheckoutReady(PaymentCheckoutReadyEvent event) {
        log.info("Publishing payment.checkout.ready for paymentId={}, orderId={}, razorpayOrderId={}",
                event.getPaymentId(), event.getOrderId(), event.getRazorpayOrderId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.PAYMENT_CHECKOUT_READY_KEY, event);
    }

    /**
     * Internal publish for Event Sourcing — payment-service self-consumes
     * payment.initiated to record the first event in the event store.
     */
    public void publishPaymentInitiated(Object payload) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.PAYMENT_INITIATED_KEY, payload);
    }
}
