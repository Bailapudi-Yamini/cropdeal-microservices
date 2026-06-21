package com.cropdeal.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefundRequest {

    @NotBlank(message = "Reason is required")
    @Size(max = 500)
    private String reason;
}
