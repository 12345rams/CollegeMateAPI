package com.collegemate.repository;

import com.collegemate.model.WalletTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface WalletTransactionRepository extends MongoRepository<WalletTransaction, String> {
    List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(String userId);
}
