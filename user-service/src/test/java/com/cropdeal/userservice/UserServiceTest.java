package com.cropdeal.userservice;

import com.cropdeal.userservice.dto.*;
import com.cropdeal.userservice.entity.OAuthProvider;
import com.cropdeal.userservice.entity.User;
import com.cropdeal.userservice.entity.UserRole;
import com.cropdeal.userservice.exception.DuplicateEntryException;
import com.cropdeal.userservice.exception.ResourceNotFoundException;
import com.cropdeal.userservice.repository.BankAccountRepository;
import com.cropdeal.userservice.repository.UserRepository;
import com.cropdeal.userservice.security.JwtUtil;
import com.cropdeal.userservice.service.EventPublisher;
import com.cropdeal.userservice.service.UserMapper;
import com.cropdeal.userservice.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock BankAccountRepository bankAccountRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock AuthenticationManager authenticationManager;
    @Mock UserMapper userMapper;
    @Mock EventPublisher eventPublisher;

    @InjectMocks UserServiceImpl userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .name("John Farmer")
                .email("john@farm.com")
                .password("encoded_pass")
                .role(UserRole.FARMER)
                .oauthProvider(OAuthProvider.EMAIL)
                .active(true)
                .build();
    }

    @Test
    void register_success() {
        RegisterRequest request = new RegisterRequest();
        request.setName("John Farmer");
        request.setEmail("john@farm.com");
        request.setPassword("password123");
        request.setRole(UserRole.FARMER);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtUtil.generateToken(any(), any())).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh_token");

        AuthResponse response = userService.register(request);

        assertThat(response.getEmail()).isEqualTo("john@farm.com");
        assertThat(response.getRole()).isEqualTo(UserRole.FARMER);
        verify(eventPublisher).publishUserRegistered(any());
    }

    @Test
    void register_duplicateEmail_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("john@farm.com");
        request.setPassword("password123");
        request.setRole(UserRole.FARMER);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateEntryException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void getProfile_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        UserResponse expected = UserResponse.builder().email("john@farm.com").build();
        when(userMapper.toUserResponse(sampleUser)).thenReturn(expected);

        UserResponse result = userService.getProfile(1L);

        assertThat(result.getEmail()).isEqualTo("john@farm.com");
    }

    @Test
    void getProfile_notFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@farm.com");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(sampleUser));
        when(jwtUtil.generateToken(any(), any())).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh_token");

        AuthResponse response = userService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access_token");
    }
}
