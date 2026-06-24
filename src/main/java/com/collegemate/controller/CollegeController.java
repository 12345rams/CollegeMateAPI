package com.collegemate.controller;

import com.collegemate.model.College;
import com.collegemate.model.Review;
import com.collegemate.model.User;
import com.collegemate.repository.CollegeRepository;
import com.collegemate.repository.ReviewRepository;
import com.collegemate.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/colleges")
public class CollegeController {

    private final CollegeRepository collegeRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public CollegeController(CollegeRepository collegeRepository, 
                             ReviewRepository reviewRepository, 
                             UserRepository userRepository) {
        this.collegeRepository = collegeRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/public")
    public List<College> getAllColleges() {
        return collegeRepository.findAll();
    }

    // Fuzzy search endpoint for autocomplete
    @GetMapping("/public/search")
    public List<College> searchColleges(@RequestParam String q) {
        if (q == null || q.trim().isEmpty()) {
            return collegeRepository.findAll();
        }
        // Escape regex special characters and search
        String escaped = q.replaceAll("[\\\\\\[\\](){}*+?^$|.]", "\\\\$0");
        return collegeRepository.searchByKeyword(escaped);
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<?> getCollegeById(@PathVariable String id) {
        return collegeRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createCollege(@RequestBody College college) {
        // Can verify if admin in real app, let's allow it for initial set up
        College saved = collegeRepository.save(college);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/public/{collegeId}/reviews")
    public List<Review> getCollegeReviews(@PathVariable String collegeId) {
        return reviewRepository.findByCollegeId(collegeId);
    }

    @PostMapping("/{collegeId}/reviews")
    public ResponseEntity<?> createReview(@PathVariable String collegeId, @RequestBody Review reviewInput, Principal principal) {
        Optional<College> collegeOpt = collegeRepository.findById(collegeId);
        if (collegeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Review review = new Review(
                collegeId,
                user.getId(),
                user.getName(),
                reviewInput.getRating(),
                reviewInput.getComment()
        );

        Review savedReview = reviewRepository.save(review);

        // Update college average rating
        College college = collegeOpt.get();
        List<Review> allReviews = reviewRepository.findByCollegeId(collegeId);
        int totalReviews = allReviews.size();
        double sum = allReviews.stream().mapToDouble(Review::getRating).sum();
        double avgRating = sum / totalReviews;

        college.setTotalReviews(totalReviews);
        college.setRating(Math.round(avgRating * 10.0) / 10.0); // round to 1 decimal place
        collegeRepository.save(college);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedReview);
    }
}
