package com.neurolearn.dto;

import java.time.OffsetDateTime;

public class SessionResponse {
    private Long id;
    private Long studentProfileId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private Integer questionsAttempted;
    private Double accuracy;

    public SessionResponse() {}

    public SessionResponse(Long id, Long studentProfileId, OffsetDateTime startTime, OffsetDateTime endTime, Integer questionsAttempted, Double accuracy) {
        this.id = id;
        this.studentProfileId = studentProfileId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.questionsAttempted = questionsAttempted;
        this.accuracy = accuracy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentProfileId() { return studentProfileId; }
    public void setStudentProfileId(Long studentProfileId) { this.studentProfileId = studentProfileId; }

    public OffsetDateTime getStartTime() { return startTime; }
    public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime; }

    public OffsetDateTime getEndTime() { return endTime; }
    public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }

    public Integer getQuestionsAttempted() { return questionsAttempted; }
    public void setQuestionsAttempted(Integer questionsAttempted) { this.questionsAttempted = questionsAttempted; }

    public Double getAccuracy() { return accuracy; }
    public void setAccuracy(Double accuracy) { this.accuracy = accuracy; }
}
