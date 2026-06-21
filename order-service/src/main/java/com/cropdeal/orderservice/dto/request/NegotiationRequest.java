package com.cropdeal.orderservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NegotiationRequest {

    @NotNull(message = "Proposed price is required")
    @Positive(message = "Proposed price must be positive")
    private Double proposedPrice;

    @Size(max = 500)
    private String message;
}
