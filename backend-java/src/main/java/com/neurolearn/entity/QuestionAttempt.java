package com.neurolearn.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "question_attempts")
public class QuestionAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile studentProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private LearningSession session;

    @Column(nullable = false)
    private Boolean correct;

    @Column(name = "response_time_ms", nullable = false)
    private Long responseTimeMs = 0L;

    @Column(name = "attempt_number")
    private Integer attemptNumber = 1;

    @Column(name = "timestamp", insertable = false, updatable = false)
    private OffsetDateTime timestamp;

    /** A–D; null for attempts recorded before V9. */
    @Column(name = "selected_option", length = 1)
    private String selectedOption;

    @Column(name = "hints_used", nullable = false)
    private Integer hintsUsed = 0;

    /** Decision that served this question (null if not served by the adaptive engine, e.g. boss fights). */
    @Column(name = "adaptive_action", length = 10)
    private String adaptiveAction;

    @Column(name = "adaptive_reason", columnDefinition = "TEXT")
    private String adaptiveReason;

    @Column(name = "difficulty_served")
    private Integer difficultyServed;

    public QuestionAttempt() {
    }

    public String getSelectedOption() { return selectedOption; }
    public void setSelectedOption(String selectedOption) { this.selectedOption = selectedOption; }
    public Integer getHintsUsed() { return hintsUsed; }
    public void setHintsUsed(Integer hintsUsed) { this.hintsUsed = hintsUsed; }
    public String getAdaptiveAction() { return adaptiveAction; }
    public void setAdaptiveAction(String adaptiveAction) { this.adaptiveAction = adaptiveAction; }
    public String getAdaptiveReason() { return adaptiveReason; }
    public void setAdaptiveReason(String adaptiveReason) { this.adaptiveReason = adaptiveReason; }
    public Integer getDifficultyServed() { return difficultyServed; }
    public void setDifficultyServed(Integer difficultyServed) { this.difficultyServed = difficultyServed; }

    public QuestionAttempt(Long id, StudentProfile studentProfile, Question question, LearningSession session, Boolean correct, Long responseTimeMs, Integer attemptNumber, OffsetDateTime timestamp) {
        this.id = id;
        this.studentProfile = studentProfile;
        this.question = question;
        this.session = session;
        this.correct = correct;
        this.responseTimeMs = responseTimeMs != null ? responseTimeMs : 0L;
        this.attemptNumber = attemptNumber != null ? attemptNumber : 1;
        this.timestamp = timestamp;
    }

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = OffsetDateTime.now();
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

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public LearningSession getSession() {
        return session;
    }

    public void setSession(LearningSession session) {
        this.session = session;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
