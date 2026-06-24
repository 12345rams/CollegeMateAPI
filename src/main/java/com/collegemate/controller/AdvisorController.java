package com.collegemate.controller;

import com.collegemate.model.AdvisorProfile;
import com.collegemate.model.User;
import com.collegemate.repository.AdvisorProfileRepository;
import com.collegemate.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/advisors")
public class AdvisorController {

    private final AdvisorProfileRepository advisorProfileRepository;
    private final UserRepository userRepository;

    public AdvisorController(AdvisorProfileRepository advisorProfileRepository, UserRepository userRepository) {
        this.advisorProfileRepository = advisorProfileRepository;
        this.userRepository = userRepository;
    }

    // Get all verified advisors who are online
    @GetMapping("/public")
    public List<AdvisorProfile> getPublicAdvisors() {
        return advisorProfileRepository.findAll().stream()
                .filter(AdvisorProfile::isVerified)
                .toList();
    }

    // Fuzzy search endpoint for autocomplete
    @GetMapping("/public/search")
    public List<AdvisorProfile> searchAdvisors(@RequestParam String q) {
        if (q == null || q.trim().isEmpty()) {
            return advisorProfileRepository.findAll().stream()
                    .filter(AdvisorProfile::isVerified)
                    .toList();
        }
        // Escape regex special characters and search
        String escaped = q.replaceAll("[\\\\\\[\\](){}*+?^$|.]", "\\\\$0");
        return advisorProfileRepository.searchVerifiedByKeyword(escaped);
    }

    // Get verified advisors for a specific college
    @GetMapping("/public/college/{collegeId}")
    public List<AdvisorProfile> getAdvisorsByCollege(@PathVariable String collegeId) {
        return advisorProfileRepository.findByCollegeIdAndIsVerified(collegeId, true);
    }

    // Get own profile (For logged-in advisor)
    @GetMapping("/profile")
    public ResponseEntity<?> getOwnProfile(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<AdvisorProfile> profileOpt = advisorProfileRepository.findByUserId(user.getId());
        if (profileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(profileOpt.get());
    }

    // Update own profile
    @PutMapping("/profile")
    public ResponseEntity<?> updateOwnProfile(@RequestBody AdvisorProfile updateInput, Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        AdvisorProfile profile = advisorProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    AdvisorProfile newProfile = new AdvisorProfile();
                    newProfile.setUserId(user.getId());
                    return newProfile;
                });

        // If collegeName changes, reset verification status and take offline
        if (updateInput.getCollegeName() != null && !updateInput.getCollegeName().equalsIgnoreCase(profile.getCollegeName())) {
            profile.setVerified(false);
            profile.setVerificationDoc(null);
            profile.setOnline(false);
        }

        profile.setCollegeId(updateInput.getCollegeId());
        profile.setCollegeName(updateInput.getCollegeName());
        profile.setMajor(updateInput.getMajor());
        profile.setEnrollmentYear(updateInput.getEnrollmentYear());
        profile.setRatePerMinute(updateInput.getRatePerMinute());
        profile.setBio(updateInput.getBio());
        profile.setSkills(updateInput.getSkills());

        AdvisorProfile saved = advisorProfileRepository.save(profile);
        return ResponseEntity.ok(saved);
    }

    // Toggle Online/Offline
    @PostMapping("/profile/status")
    public ResponseEntity<?> toggleStatus(@RequestParam boolean online, Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        AdvisorProfile profile = advisorProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Advisor profile not found"));

        profile.setOnline(online);
        advisorProfileRepository.save(profile);

        return ResponseEntity.ok(profile);
    }

    // Upload verification document / Request verification
    @PostMapping("/profile/verify")
    public ResponseEntity<?> submitVerification(@RequestBody String verificationDocBase64, Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        AdvisorProfile profile = advisorProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Advisor profile not found"));

        profile.setVerificationDoc(verificationDocBase64);
        profile.setVerified(false); // Reset/Keep unverified until approved
        profile.setOnline(false); // Take offline during verification
        advisorProfileRepository.save(profile);

        return ResponseEntity.ok(profile);
    }

    // Admin: List all pending verifications (where verificationDoc is present but isVerified is false)
    @GetMapping("/admin/pending")
    public List<com.collegemate.dto.PendingVerificationResponse> getPendingVerifications() {
        return advisorProfileRepository.findAll().stream()
                .filter(profile -> !profile.isVerified() && profile.getVerificationDoc() != null && !profile.getVerificationDoc().isEmpty())
                .map(profile -> {
                    User user = userRepository.findById(profile.getUserId()).orElse(null);
                    return new com.collegemate.dto.PendingVerificationResponse(profile, user);
                })
                .toList();
    }

    // Admin: Approve or reject verification
    @PostMapping("/admin/verify/{profileId}")
    public ResponseEntity<?> verifyAdvisor(@PathVariable String profileId, @RequestParam boolean approve) {
        Optional<AdvisorProfile> profileOpt = advisorProfileRepository.findById(profileId);
        if (profileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AdvisorProfile profile = profileOpt.get();
        if (approve) {
            profile.setVerified(true);
        } else {
            profile.setVerified(false);
            profile.setVerificationDoc(null); // Clear document to allow re-submission
        }

        advisorProfileRepository.save(profile);
        return ResponseEntity.ok(profile);
    }
}
