package com.cropdeal.paymentservice.service;

import com.cropdeal.paymentservice.dto.response.PaymentEventResponse;
import com.cropdeal.paymentservice.dto.response.PaymentResponse;
import com.cropdeal.paymentservice.dto.response.ReceiptResponse;
import com.cropdeal.paymentservice.entity.Payment;
import com.cropdeal.paymentservice.entity.PaymentEvent;
import com.cropdeal.paymentservice.entity.Receipt;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-21T11:46:47+0530",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class PaymentMapperImpl implements PaymentMapper {

    @Override
    public PaymentResponse toPaymentResponse(Payment payment) {
        if ( payment == null ) {
            return null;
        }

        PaymentResponse.PaymentResponseBuilder paymentResponse = PaymentResponse.builder();

        paymentResponse.amount( payment.getAmount() );
        paymentResponse.createdAt( payment.getCreatedAt() );
        paymentResponse.dealerId( payment.getDealerId() );
        paymentResponse.farmerId( payment.getFarmerId() );
        paymentResponse.id( payment.getId() );
        paymentResponse.orderId( payment.getOrderId() );
        paymentResponse.paidAt( payment.getPaidAt() );
        paymentResponse.paymentGatewayRef( payment.getPaymentGatewayRef() );
        paymentResponse.razorpayOrderId( payment.getRazorpayOrderId() );
        paymentResponse.status( payment.getStatus() );
        paymentResponse.transactionId( payment.getTransactionId() );
        paymentResponse.updatedAt( payment.getUpdatedAt() );

        return paymentResponse.build();
    }

    @Override
    public PaymentEventResponse toEventResponse(PaymentEvent event) {
        if ( event == null ) {
            return null;
        }

        PaymentEventResponse.PaymentEventResponseBuilder paymentEventResponse = PaymentEventResponse.builder();

        paymentEventResponse.eventType( event.getEventType() );
        paymentEventResponse.id( event.getId() );
        paymentEventResponse.occurredAt( event.getOccurredAt() );
        paymentEventResponse.payload( event.getPayload() );
        paymentEventResponse.paymentId( event.getPaymentId() );

        return paymentEventResponse.build();
    }

    @Override
    public ReceiptResponse toReceiptResponse(Receipt receipt) {
        if ( receipt == null ) {
            return null;
        }

        ReceiptResponse.ReceiptResponseBuilder receiptResponse = ReceiptResponse.builder();

        receiptResponse.amount( receipt.getAmount() );
        receiptResponse.cropDetails( receipt.getCropDetails() );
        receiptResponse.dealerId( receipt.getDealerId() );
        receiptResponse.farmerId( receipt.getFarmerId() );
        receiptResponse.generatedAt( receipt.getGeneratedAt() );
        receiptResponse.id( receipt.getId() );
        receiptResponse.paymentId( receipt.getPaymentId() );
        receiptResponse.receiptNumber( receipt.getReceiptNumber() );

        return receiptResponse.build();
    }
}
