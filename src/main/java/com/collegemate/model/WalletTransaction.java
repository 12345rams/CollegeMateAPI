package com.collegemate.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "wallet_transactions")
public class WalletTransaction {
    @Id
    private String id;
    private String userId;
    private TransactionType type;
    private double amount;
    private String description;
    private Instant createdAt;

    public enum TransactionType {
        DEPOSIT,
        DEDUCTION,
        EARNING,
        WITHDRAWAL
    }

    public WalletTransaction() {
        this.createdAt = Instant.now();
    }

    public WalletTransaction(String userId, TransactionType type, double amount, String description) {
        this();
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.description = description;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
