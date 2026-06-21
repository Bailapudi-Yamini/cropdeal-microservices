package com.cropdeal.paymentservice.service;

import com.cropdeal.paymentservice.dto.response.PaymentEventResponse;
import com.cropdeal.paymentservice.dto.response.PaymentResponse;
import com.cropdeal.paymentservice.dto.response.ReceiptResponse;
import com.cropdeal.paymentservice.entity.Payment;
import com.cropdeal.paymentservice.entity.PaymentEvent;
import com.cropdeal.paymentservice.entity.Receipt;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    PaymentResponse toPaymentResponse(Payment payment);
    PaymentEventResponse toEventResponse(PaymentEvent event);
    ReceiptResponse toReceiptResponse(Receipt receipt);
}
