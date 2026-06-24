package com.collegemate.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "users")
public class User {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String email;
    
    private String password;
    private String name;
    private Role role;
    private AuthProvider authProvider;
    private double walletBalance;
    private Instant createdAt;

    public enum Role {
        SEEKER,
        ADVISOR,
        ADMIN,
        BUSINESS,
        business,
        CUSTOMER,
        customer
    }

    public enum AuthProvider {
        LOCAL,
        GOOGLE
    }

    public User() {
        this.createdAt = Instant.now();
        this.walletBalance = 0.0;
        this.authProvider = AuthProvider.LOCAL;
    }

    public User(String email, String password, String name, Role role) {
        this();
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
    }

    public User(String email, String name, Role role, AuthProvider authProvider) {
        this();
        this.email = email;
        this.name = name;
        this.role = role;
        this.authProvider = authProvider;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public double getWalletBalance() { return walletBalance; }
    public void setWalletBalance(double walletBalance) { this.walletBalance = walletBalance; }

    public AuthProvider getAuthProvider() { return authProvider; }
    public void setAuthProvider(AuthProvider authProvider) { this.authProvider = authProvider; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
