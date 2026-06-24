package com.collegemate.repository;

import com.collegemate.model.College;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface CollegeRepository extends MongoRepository<College, String> {

    // Fuzzy search: case-insensitive regex matching on name, location, and description
    @Query("{ '$or': [ " +
            "{ 'name': { '$regex': ?0, '$options': 'i' } }, " +
            "{ 'location': { '$regex': ?0, '$options': 'i' } }, " +
            "{ 'description': { '$regex': ?0, '$options': 'i' } } " +
            "] }")
    List<College> searchByKeyword(String keyword);
}
