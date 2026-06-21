package com.cropdeal.orderservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PlaceOrderRequest {

    @NotNull(message = "Crop listing ID is required")
    private Long cropListingId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Double quantity;

    @Size(max = 500)
    private String dealerNotes;
}
