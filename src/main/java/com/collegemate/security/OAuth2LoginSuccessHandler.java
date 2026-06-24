package com.collegemate.security;

import com.collegemate.model.User;
import com.collegemate.model.User.AuthProvider;
import com.collegemate.model.User.Role;
import com.collegemate.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    @Value("${collegemate.frontend.url}")
    private String frontendUrl;

    public OAuth2LoginSuccessHandler(UserRepository userRepository, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (email == null) {
            response.sendRedirect(frontendUrl + "/login?error=" +
                    URLEncoder.encode("Google account email not available", StandardCharsets.UTF_8));
            return;
        }

        // Find or create user
        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            // Update name if it changed on Google side
            if (name != null && !name.equals(user.getName())) {
                user.setName(name);
                userRepository.save(user);
            }
        } else {
            // Auto-register as SEEKER
            user = new User(email, name != null ? name : email, Role.SEEKER, AuthProvider.GOOGLE);
            user = userRepository.save(user);
        }

        // Generate JWT
        String token = tokenProvider.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getId(),
                user.getName()
        );

        // Redirect to frontend with token
        String redirectUrl = frontendUrl + "/oauth2/callback?token=" +
                URLEncoder.encode(token, StandardCharsets.UTF_8) +
                "&userId=" + URLEncoder.encode(user.getId(), StandardCharsets.UTF_8) +
                "&name=" + URLEncoder.encode(user.getName(), StandardCharsets.UTF_8) +
                "&email=" + URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8) +
                "&role=" + URLEncoder.encode(user.getRole().name(), StandardCharsets.UTF_8) +
                "&walletBalance=" + user.getWalletBalance();

        response.sendRedirect(redirectUrl);
    }
}
