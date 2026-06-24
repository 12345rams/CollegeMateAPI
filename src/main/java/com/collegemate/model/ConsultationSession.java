package com.collegemate.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "sessions")
public class ConsultationSession {
    @Id
    private String id;
    private String seekerId;
    private String advisorId;
    private SessionType type;
    private SessionStatus status;
    private Instant startTime;
    private Instant endTime;
    private long durationSeconds;
    private double amountCharged;
    private Instant createdAt;

    // Scheduling and Duration Billing Fields
    private int selectedDurationMinutes = 5; // default to 5 minutes
    private boolean isScheduled = false;
    private Instant scheduledTime;
    private boolean upfrontCharged = false;
    private boolean extensionActive = false;

    public enum SessionType {
        CHAT,
        VIDEO
    }

    public enum SessionStatus {
        PENDING,
        ACTIVE,
        COMPLETED,
        REJECTED,
        TIMEOUT,
        SCHEDULED,
        CANCELLED
    }

    public ConsultationSession() {
        this.createdAt = Instant.now();
        this.status = SessionStatus.PENDING;
        this.durationSeconds = 0;
        this.amountCharged = 0.0;
        this.selectedDurationMinutes = 5;
        this.isScheduled = false;
        this.upfrontCharged = false;
        this.extensionActive = false;
    }

    public ConsultationSession(String seekerId, String advisorId, SessionType type) {
        this();
        this.seekerId = seekerId;
        this.advisorId = advisorId;
        this.type = type;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSeekerId() { return seekerId; }
    public void setSeekerId(String seekerId) { this.seekerId = seekerId; }

    public String getAdvisorId() { return advisorId; }
    public void setAdvisorId(String advisorId) { this.advisorId = advisorId; }

    public SessionType getType() { return type; }
    public void setType(SessionType type) { this.type = type; }

    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }

    public double getAmountCharged() { return amountCharged; }
    public void setAmountCharged(double amountCharged) { this.amountCharged = amountCharged; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public int getSelectedDurationMinutes() { return selectedDurationMinutes; }
    public void setSelectedDurationMinutes(int selectedDurationMinutes) { this.selectedDurationMinutes = selectedDurationMinutes; }

    public boolean isScheduled() { return isScheduled; }
    public void setScheduled(boolean scheduled) { isScheduled = scheduled; }

    public Instant getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(Instant scheduledTime) { this.scheduledTime = scheduledTime; }

    public boolean isUpfrontCharged() { return upfrontCharged; }
    public void setUpfrontCharged(boolean upfrontCharged) { this.upfrontCharged = upfrontCharged; }

    public boolean isExtensionActive() { return extensionActive; }
    public void setExtensionActive(boolean extensionActive) { this.extensionActive = extensionActive; }
}
