package com.collegemate.controller;

import com.collegemate.model.User;
import com.collegemate.model.User.Role;
import com.collegemate.model.WalletTransaction;
import com.collegemate.model.WalletTransaction.TransactionType;
import com.collegemate.repository.UserRepository;
import com.collegemate.repository.WalletTransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final UserRepository userRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletController(UserRepository userRepository, WalletTransactionRepository walletTransactionRepository) {
        this.userRepository = userRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(user.getWalletBalance());
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestParam double amount, Principal principal) {
        if (amount <= 0) {
            return ResponseEntity.badRequest().body("Amount must be greater than zero.");
        }

        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setWalletBalance(user.getWalletBalance() + amount);
        userRepository.save(user);

        WalletTransaction tx = new WalletTransaction(
                user.getId(),
                TransactionType.DEPOSIT,
                amount,
                "Deposited funds via portal"
        );
        walletTransactionRepository.save(tx);

        return ResponseEntity.ok(user);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestParam double amount, Principal principal) {
        if (amount <= 0) {
            return ResponseEntity.badRequest().body("Amount must be greater than zero.");
        }

        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.ADVISOR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only advisors can withdraw earnings.");
        }

        if (user.getWalletBalance() < amount) {
            return ResponseEntity.badRequest().body("Insufficient balance for withdrawal.");
        }

        user.setWalletBalance(user.getWalletBalance() - amount);
        userRepository.save(user);

        WalletTransaction tx = new WalletTransaction(
                user.getId(),
                TransactionType.WITHDRAWAL,
                amount,
                "Withdrew earnings from portal"
        );
        walletTransactionRepository.save(tx);

        return ResponseEntity.ok(user);
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<WalletTransaction> transactions = walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return ResponseEntity.ok(transactions);
    }
}
