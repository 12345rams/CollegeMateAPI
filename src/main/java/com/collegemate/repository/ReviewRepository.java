package com.collegemate.repository;

import com.collegemate.model.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByCollegeId(String collegeId);
}
