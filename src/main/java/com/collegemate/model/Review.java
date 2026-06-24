package com.collegemate.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "reviews")
public class Review {
    @Id
    private String id;
    private String collegeId;
    private String seekerId;
    private String seekerName;
    private int rating; // 1-5
    private String comment;
    private Instant createdAt;

    public Review() {
        this.createdAt = Instant.now();
    }

    public Review(String collegeId, String seekerId, String seekerName, int rating, String comment) {
        this();
        this.collegeId = collegeId;
        this.seekerId = seekerId;
        this.seekerName = seekerName;
        this.rating = rating;
        this.comment = comment;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getSeekerId() { return seekerId; }
    public void setSeekerId(String seekerId) { this.seekerId = seekerId; }

    public String getSeekerName() { return seekerName; }
    public void setSeekerName(String seekerName) { this.seekerName = seekerName; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
