package com.cropdeal.orderservice.service;

import com.cropdeal.orderservice.dto.response.NegotiationResponse;
import com.cropdeal.orderservice.dto.response.OrderResponse;
import com.cropdeal.orderservice.entity.Negotiation;
import com.cropdeal.orderservice.entity.Order;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-21T12:33:41+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.2 (Oracle Corporation)"
)
@Component
public class OrderMapperImpl implements OrderMapper {

    @Override
    public OrderResponse toOrderResponse(Order order) {
        if ( order == null ) {
            return null;
        }

        OrderResponse.OrderResponseBuilder orderResponse = OrderResponse.builder();

        orderResponse.id( order.getId() );
        orderResponse.cropListingId( order.getCropListingId() );
        orderResponse.farmerId( order.getFarmerId() );
        orderResponse.dealerId( order.getDealerId() );
        orderResponse.quantity( order.getQuantity() );
        orderResponse.agreedPricePerUnit( order.getAgreedPricePerUnit() );
        orderResponse.totalAmount( order.getTotalAmount() );
        orderResponse.status( order.getStatus() );
        orderResponse.dealerNotes( order.getDealerNotes() );
        orderResponse.negotiationRounds( order.getNegotiationRounds() );
        orderResponse.createdAt( order.getCreatedAt() );
        orderResponse.updatedAt( order.getUpdatedAt() );

        return orderResponse.build();
    }

    @Override
    public NegotiationResponse toNegotiationResponse(Negotiation negotiation) {
        if ( negotiation == null ) {
            return null;
        }

        NegotiationResponse.NegotiationResponseBuilder negotiationResponse = NegotiationResponse.builder();

        negotiationResponse.orderId( negotiationOrderId( negotiation ) );
        negotiationResponse.id( negotiation.getId() );
        negotiationResponse.initiatedBy( negotiation.getInitiatedBy() );
        negotiationResponse.proposedPrice( negotiation.getProposedPrice() );
        negotiationResponse.message( negotiation.getMessage() );
        negotiationResponse.status( negotiation.getStatus() );
        negotiationResponse.createdAt( negotiation.getCreatedAt() );

        negotiationResponse.proposedBy( resolveProposedBy(negotiation) );

        return negotiationResponse.build();
    }

    private Long negotiationOrderId(Negotiation negotiation) {
        if ( negotiation == null ) {
            return null;
        }
        Order order = negotiation.getOrder();
        if ( order == null ) {
            return null;
        }
        Long id = order.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
