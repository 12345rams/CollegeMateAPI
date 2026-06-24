package com.collegemate.controller;

import com.collegemate.dto.AuthRequest;
import com.collegemate.dto.AuthResponse;
import com.collegemate.dto.RegisterRequest;
import com.collegemate.model.AdvisorProfile;
import com.collegemate.model.User;
import com.collegemate.model.User.Role;
import com.collegemate.repository.AdvisorProfileRepository;
import com.collegemate.repository.UserRepository;
import com.collegemate.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final AdvisorProfileRepository advisorProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserRepository userRepository, 
                          AdvisorProfileRepository advisorProfileRepository, 
                          PasswordEncoder passwordEncoder, 
                          JwtTokenProvider tokenProvider, 
                          AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.advisorProfileRepository = advisorProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email is already registered.");
        }

        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getName(),
                request.getRole()
        );

        User savedUser = userRepository.save(user);

        // If the user is an advisor, create a default profile
        if (savedUser.getRole() == Role.ADVISOR) {
            AdvisorProfile profile = new AdvisorProfile();
            profile.setUserId(savedUser.getId());
            profile.setCollegeName("");
            profile.setMajor("");
            profile.setBio("");
            profile.setVerified(false);
            profile.setOnline(false);
            advisorProfileRepository.save(profile);
        }

        String token = tokenProvider.generateToken(savedUser.getEmail(), savedUser.getRole().name(), savedUser.getId(), savedUser.getName());
        return ResponseEntity.ok(new AuthResponse(token, savedUser.getRole().name(), savedUser.getId(), savedUser.getName(), savedUser.getEmail(), savedUser.getWalletBalance()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user != null && user.getRole() == Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin login not allowed here.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        if (user == null) {
            user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found after authentication success"));
        }

        String token = tokenProvider.generateToken(user.getEmail(), user.getRole().name(), user.getId(), user.getName());
        return ResponseEntity.ok(new AuthResponse(token, user.getRole().name(), user.getId(), user.getName(), user.getEmail(), user.getWalletBalance()));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied. Not an admin.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = tokenProvider.generateToken(user.getEmail(), user.getRole().name(), user.getId(), user.getName());
        return ResponseEntity.ok(new AuthResponse(token, user.getRole().name(), user.getId(), user.getName(), user.getEmail(), user.getWalletBalance()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<User> userOpt = userRepository.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }
        User user = userOpt.get();
        return ResponseEntity.ok(user);
    }
}
