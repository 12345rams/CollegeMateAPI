package com.collegemate.repository;

import com.collegemate.model.AdvisorProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AdvisorProfileRepository extends MongoRepository<AdvisorProfile, String> {
    Optional<AdvisorProfile> findByUserId(String userId);
    List<AdvisorProfile> findByCollegeIdAndIsVerified(String collegeId, boolean isVerified);
    List<AdvisorProfile> findByIsVerified(boolean isVerified);

    // Fuzzy search: case-insensitive regex matching on collegeName, major, bio, and skills
    @Query("{ 'isVerified': true, '$or': [ " +
            "{ 'collegeName': { '$regex': ?0, '$options': 'i' } }, " +
            "{ 'major': { '$regex': ?0, '$options': 'i' } }, " +
            "{ 'bio': { '$regex': ?0, '$options': 'i' } }, " +
            "{ 'skills': { '$regex': ?0, '$options': 'i' } } " +
            "] }")
    List<AdvisorProfile> searchVerifiedByKeyword(String keyword);
}
