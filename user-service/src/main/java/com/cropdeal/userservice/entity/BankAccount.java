package com.cropdeal.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bank_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false, length = 11)
    private String ifscCode;

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private String accountHolderName;
}
