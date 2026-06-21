package com.cropdeal.userservice.service;

import com.cropdeal.userservice.dto.*;
import com.cropdeal.userservice.entity.UserRole;

import java.util.List;

public interface UserService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse getProfile(Long userId);

    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    void deactivateUser(Long userId);

    List<UserResponse> getUsersByRole(UserRole role);

    PagedResponse<UserResponse> getUsersPaged(int page, int size, String role);

    UserResponse toggleUserStatus(Long userId);

    BankAccountResponse addBankAccount(Long userId, BankAccountRequest request);

    List<BankAccountResponse> getBankAccounts(Long userId);
}
