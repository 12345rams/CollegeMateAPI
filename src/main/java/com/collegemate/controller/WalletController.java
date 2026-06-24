package com.collegemate.controller;

import com.collegemate.model.User;
import com.collegemate.model.User.Role;
import com.collegemate.model.WalletTransaction;
import com.collegemate.model.WalletTransaction.TransactionType;
import com.collegemate.repository.UserRepository;
import com.collegemate.repository.WalletTransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final UserRepository userRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    public WalletController(UserRepository userRepository, WalletTransactionRepository walletTransactionRepository) {
        this.userRepository = userRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @PostMapping("/razorpay/order")
    public ResponseEntity<?> createRazorpayOrder(@RequestParam double amount, Principal principal) {
        if (amount <= 0) {
            return ResponseEntity.badRequest().body("Amount must be greater than zero.");
        }

        try {
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int) (amount * 100)); // amount in the smallest currency unit (paise)
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

            Order order = razorpay.orders.create(orderRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.get("id"));
            response.put("amount", order.get("amount"));
            response.put("currency", order.get("currency"));
            response.put("keyId", razorpayKeyId);

            return ResponseEntity.ok(response);
        } catch (RazorpayException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating Razorpay order: " + e.getMessage());
        }
    }

    @PostMapping("/razorpay/verify")
    public ResponseEntity<?> verifyRazorpayPayment(@RequestBody Map<String, String> data, Principal principal) {
        String razorpayPaymentId = data.get("razorpay_payment_id");
        String razorpayOrderId = data.get("razorpay_order_id");
        String razorpaySignature = data.get("razorpay_signature");
        double amount = Double.parseDouble(data.get("amount")); // Pass the amount from frontend to credit

        if (amount <= 0) {
            return ResponseEntity.badRequest().body("Amount must be greater than zero.");
        }

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_signature", razorpaySignature);

            boolean isValid = Utils.verifyPaymentSignature(options, razorpayKeySecret);

            if (isValid) {
                User user = userRepository.findByEmail(principal.getName())
                        .orElseThrow(() -> new RuntimeException("User not found"));

                user.setWalletBalance(user.getWalletBalance() + amount);
                userRepository.save(user);

                WalletTransaction tx = new WalletTransaction(
                        user.getId(),
                        TransactionType.DEPOSIT,
                        amount,
                        "Deposited funds via Razorpay (Txn: " + razorpayPaymentId + ")"
                );
                walletTransactionRepository.save(tx);

                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment signature verification failed.");
            }
        } catch (RazorpayException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error verifying Razorpay signature: " + e.getMessage());
        }
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
