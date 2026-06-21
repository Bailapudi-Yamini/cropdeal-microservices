package com.cropdeal.userservice.security;

import com.cropdeal.userservice.entity.OAuthProvider;
import com.cropdeal.userservice.entity.User;
import com.cropdeal.userservice.entity.UserRole;
import com.cropdeal.userservice.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String providerId = oAuth2User.getAttribute("id");

        if (email == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not provided by OAuth2 provider");
            return;
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .name(name)
                    .role(UserRole.DEALER)
                    .oauthProvider(OAuthProvider.FACEBOOK)
                    .oauthProviderId(providerId)
                    .active(true)
                    .build();
            return userRepository.save(newUser);
        });

        var userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password("")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .build();

        String token = jwtUtil.generateToken(userDetails,
                Map.of("role", user.getRole(), "userId", user.getId()));

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        // Use ObjectMapper to safely serialize — prevents XSS via malformed token/data
        objectMapper.writeValue(response.getWriter(), Map.of(
                "token", token,
                "tokenType", "Bearer",
                "userId", user.getId(),
                "role", user.getRole()
        ));
    }
}
