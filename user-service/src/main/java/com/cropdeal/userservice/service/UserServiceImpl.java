package com.cropdeal.userservice.service;

import com.cropdeal.userservice.dto.*;
import com.cropdeal.userservice.entity.BankAccount;
import com.cropdeal.userservice.entity.OAuthProvider;
import com.cropdeal.userservice.entity.User;
import com.cropdeal.userservice.entity.UserRole;
import com.cropdeal.userservice.exception.DuplicateEntryException;
import com.cropdeal.userservice.exception.InvalidCredentialsException;
import com.cropdeal.userservice.exception.ResourceNotFoundException;
import com.cropdeal.userservice.repository.BankAccountRepository;
import com.cropdeal.userservice.repository.UserRepository;
import com.cropdeal.userservice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final EventPublisher eventPublisher;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEntryException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole())
                .oauthProvider(OAuthProvider.EMAIL)
                .active(true)
                .build();

        user = userRepository.save(user);
        eventPublisher.publishUserRegistered(Map.of("userId", user.getId(), "email", user.getEmail(), "role", user.getRole()));

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", 0L));

        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return userMapper.toUserResponse(findUserById(userId));
    }

    @Override
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        if (request.getName() != null) user.setName(request.getName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getProfileImageUrl() != null) user.setProfileImageUrl(request.getProfileImageUrl());

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    public void deactivateUser(Long userId) {
        User user = findUserById(userId);
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getUsersPaged(int page, int size, String role) {
        var pageable = PageRequest.of(page, size);
        var pageResult = (role == null)
                ? userRepository.findAll(pageable)
                : userRepository.findByRole(UserRole.valueOf(role), pageable);
        return PagedResponse.from(pageResult.map(userMapper::toUserResponse));
    }

    @Override
    public UserResponse toggleUserStatus(Long userId) {
        User user = findUserById(userId);
        user.setActive(!user.isActive());
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role).stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    @Override
    public BankAccountResponse addBankAccount(Long userId, BankAccountRequest request) {
        if (bankAccountRepository.existsByAccountNumber(request.getAccountNumber())) {
            throw new DuplicateEntryException("Account number already exists");
        }

        User user = findUserById(userId);
        BankAccount account = BankAccount.builder()
                .user(user)
                .accountNumber(request.getAccountNumber())
                .ifscCode(request.getIfscCode())
                .bankName(request.getBankName())
                .accountHolderName(request.getAccountHolderName())
                .build();

        return userMapper.toBankAccountResponse(bankAccountRepository.save(account));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccountResponse> getBankAccounts(Long userId) {
        findUserById(userId); // validate user exists
        return bankAccountRepository.findByUserId(userId).stream()
                .map(userMapper::toBankAccountResponse)
                .toList();
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private AuthResponse buildAuthResponse(User user) {
        var userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword() != null ? user.getPassword() : "")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .build();

        String accessToken = jwtUtil.generateToken(userDetails,
                Map.of("role", user.getRole(), "userId", user.getId()));
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
