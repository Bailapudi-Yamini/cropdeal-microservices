package com.cropdeal.userservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BankAccountResponse {
    private Long id;
    private Long userId;
    private String accountNumber;
    private String ifscCode;
    private String bankName;
    private String accountHolderName;
}
