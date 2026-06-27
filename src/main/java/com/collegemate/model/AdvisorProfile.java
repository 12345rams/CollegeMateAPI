package com.collegemate.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "advisor_profiles")
public class AdvisorProfile {
    @Id
    private String id;
    private String userId;
    private String name;
    private String collegeId;
    private String collegeName;
    private String collegeLocation;
    private String major;
    private int enrollmentYear;
    private double ratePerMinute;
    private double chatRatePerMinute;
    private double videoRatePerMinute;
    private String bio;
    private List<String> skills;
    private boolean isVerified;
    private String verificationDoc;
    private boolean isOnline;
    private double rating;
    private int totalReviews;

    public AdvisorProfile() {
        this.skills = new ArrayList<>();
        this.isVerified = false;
        this.isOnline = false;
        this.rating = 0.0;
        this.totalReviews = 0;
        this.ratePerMinute = 0.0;
        this.chatRatePerMinute = 0.0;
        this.videoRatePerMinute = 0.0;
    }

    public AdvisorProfile(String userId, String collegeId, String collegeName, String major, int enrollmentYear, double ratePerMinute, String bio) {
        this();
        this.userId = userId;
        this.collegeId = collegeId;
        this.collegeName = collegeName;
        this.major = major;
        this.enrollmentYear = enrollmentYear;
        this.ratePerMinute = ratePerMinute;
        this.chatRatePerMinute = ratePerMinute;
        this.videoRatePerMinute = ratePerMinute;
        this.bio = bio;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getCollegeName() { return collegeName; }
    public void setCollegeName(String collegeName) { this.collegeName = collegeName; }

    public String getCollegeLocation() { return collegeLocation; }
    public void setCollegeLocation(String collegeLocation) { this.collegeLocation = collegeLocation; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }

    public int getEnrollmentYear() { return enrollmentYear; }
    public void setEnrollmentYear(int enrollmentYear) { this.enrollmentYear = enrollmentYear; }

    public double getRatePerMinute() { return ratePerMinute; }
    public void setRatePerMinute(double ratePerMinute) { this.ratePerMinute = ratePerMinute; }

    public double getChatRatePerMinute() { return chatRatePerMinute > 0 ? chatRatePerMinute : ratePerMinute; }
    public void setChatRatePerMinute(double chatRatePerMinute) { this.chatRatePerMinute = chatRatePerMinute; }

    public double getVideoRatePerMinute() { return videoRatePerMinute > 0 ? videoRatePerMinute : ratePerMinute; }
    public void setVideoRatePerMinute(double videoRatePerMinute) { this.videoRatePerMinute = videoRatePerMinute; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public String getVerificationDoc() { return verificationDoc; }
    public void setVerificationDoc(String verificationDoc) { this.verificationDoc = verificationDoc; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getTotalReviews() { return totalReviews; }
    public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }
}
