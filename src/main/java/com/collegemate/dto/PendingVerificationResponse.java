package com.collegemate.dto;

import com.collegemate.model.AdvisorProfile;
import com.collegemate.model.User;

import java.util.List;

public class PendingVerificationResponse {
    private String id;
    private String userId;
    private String name;
    private String email;
    private String collegeName;
    private String major;
    private int enrollmentYear;
    private double ratePerMinute;
    private double chatRatePerMinute;
    private double videoRatePerMinute;
    private String bio;
    private List<String> skills;
    private String verificationDoc;

    public PendingVerificationResponse(AdvisorProfile profile, User user) {
        this.id = profile.getId();
        this.userId = profile.getUserId();
        this.collegeName = profile.getCollegeName();
        this.major = profile.getMajor();
        this.enrollmentYear = profile.getEnrollmentYear();
        this.ratePerMinute = profile.getRatePerMinute();
        this.chatRatePerMinute = profile.getChatRatePerMinute();
        this.videoRatePerMinute = profile.getVideoRatePerMinute();
        this.bio = profile.getBio();
        this.skills = profile.getSkills();
        this.verificationDoc = profile.getVerificationDoc();
        if (user != null) {
            this.name = user.getName();
            this.email = user.getEmail();
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCollegeName() { return collegeName; }
    public void setCollegeName(String collegeName) { this.collegeName = collegeName; }

    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }

    public int getEnrollmentYear() { return enrollmentYear; }
    public void setEnrollmentYear(int enrollmentYear) { this.enrollmentYear = enrollmentYear; }

    public double getRatePerMinute() { return ratePerMinute; }
    public void setRatePerMinute(double ratePerMinute) { this.ratePerMinute = ratePerMinute; }

    public double getChatRatePerMinute() { return chatRatePerMinute; }
    public void setChatRatePerMinute(double chatRatePerMinute) { this.chatRatePerMinute = chatRatePerMinute; }

    public double getVideoRatePerMinute() { return videoRatePerMinute; }
    public void setVideoRatePerMinute(double videoRatePerMinute) { this.videoRatePerMinute = videoRatePerMinute; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public String getVerificationDoc() { return verificationDoc; }
    public void setVerificationDoc(String verificationDoc) { this.verificationDoc = verificationDoc; }
}
