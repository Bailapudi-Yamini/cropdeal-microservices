package com.cropdeal.orderservice.service;

import com.cropdeal.orderservice.dto.response.NegotiationResponse;
import com.cropdeal.orderservice.dto.response.OrderResponse;
import com.cropdeal.orderservice.entity.Negotiation;
import com.cropdeal.orderservice.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toOrderResponse(Order order);

    @Mapping(target = "orderId",    source = "order.id")
    @Mapping(target = "proposedBy", expression = "java(resolveProposedBy(negotiation))")
    NegotiationResponse toNegotiationResponse(Negotiation negotiation);

    /** Returns "FARMER" if the negotiation was initiated by the farmer, "DEALER" otherwise */
    default String resolveProposedBy(Negotiation negotiation) {
        if (negotiation.getOrder() == null) return "UNKNOWN";
        return negotiation.getOrder().getFarmerId().equals(negotiation.getInitiatedBy())
                ? "FARMER"
                : "DEALER";
    }
}
