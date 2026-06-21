package com.cropdeal.userservice.repository;

import com.cropdeal.userservice.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findByUserId(Long userId);

    Optional<BankAccount> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);
}
