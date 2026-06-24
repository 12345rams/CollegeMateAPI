package com.collegemate.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "colleges")
public class College {
    @Id
    private String id;
    private String name;
    private String location;
    private String description;
    private String logoUrl;
    private double rating;
    private int totalReviews;

    public College() {
        this.rating = 0.0;
        this.totalReviews = 0;
    }

    public College(String name, String location, String description, String logoUrl) {
        this();
        this.name = name;
        this.location = location;
        this.description = description;
        this.logoUrl = logoUrl;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getTotalReviews() { return totalReviews; }
    public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }
}
