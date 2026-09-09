package com.neurolearn.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "topic_progress", uniqueConstraints = {
    @UniqueConstraint(name = "uk_student_topic", columnNames = {"student_id", "topic"})
})
public class TopicProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile studentProfile;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "mastery_score")
    private Double masteryScore = 0.0;

    private Integer attempts = 0;

    @Column(name = "correct_count")
    private Integer correctCount = 0;

    public TopicProgress() {
    }

    public TopicProgress(Long id, StudentProfile studentProfile, String topic, Double masteryScore, Integer attempts, Integer correctCount) {
        this.id = id;
        this.studentProfile = studentProfile;
        this.topic = topic;
        this.masteryScore = masteryScore != null ? masteryScore : 0.0;
        this.attempts = attempts != null ? attempts : 0;
        this.correctCount = correctCount != null ? correctCount : 0;
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

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Double getMasteryScore() {
        return masteryScore;
    }

    public void setMasteryScore(Double masteryScore) {
        this.masteryScore = masteryScore;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public Integer getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(Integer correctCount) {
        this.correctCount = correctCount;
    }
}
