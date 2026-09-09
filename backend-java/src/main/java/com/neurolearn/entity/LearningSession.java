package com.neurolearn.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "learning_sessions")
public class LearningSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile studentProfile;

    @Column(name = "start_time", insertable = false, updatable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time")
    private OffsetDateTime endTime;

    @Column(name = "questions_attempted")
    private Integer questionsAttempted = 0;

    private Double accuracy = 0.0;

    public LearningSession() {
    }

    public LearningSession(Long id, StudentProfile studentProfile, OffsetDateTime startTime, OffsetDateTime endTime, Integer questionsAttempted, Double accuracy) {
        this.id = id;
        this.studentProfile = studentProfile;
        this.startTime = startTime;
        this.endTime = endTime;
        this.questionsAttempted = questionsAttempted != null ? questionsAttempted : 0;
        this.accuracy = accuracy != null ? accuracy : 0.0;
    }

    @PrePersist
    protected void onCreate() {
        if (startTime == null) {
            startTime = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StudentProfile getStudentProfile() {
        return studentProfile;
    }

    public void setStudentProfile(StudentProfile studentProfile) {
        this.studentProfile = studentProfile;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(OffsetDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getQuestionsAttempted() {
        return questionsAttempted;
    }

    public void setQuestionsAttempted(Integer questionsAttempted) {
        this.questionsAttempted = questionsAttempted;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }
}
