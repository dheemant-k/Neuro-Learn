package com.neurolearn.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "student_profiles")
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "current_level")
    private Integer currentLevel = 1;

    @Column(name = "overall_accuracy")
    private Double overallAccuracy = 0.0;

    @Column(name = "total_xp")
    private Integer totalXp = 0;

    public StudentProfile() {
    }

    public StudentProfile(Long id, User user, Integer currentLevel, Double overallAccuracy, Integer totalXp) {
        this.id = id;
        this.user = user;
        this.currentLevel = currentLevel != null ? currentLevel : 1;
        this.overallAccuracy = overallAccuracy != null ? overallAccuracy : 0.0;
        this.totalXp = totalXp != null ? totalXp : 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(Integer currentLevel) {
        this.currentLevel = currentLevel;
    }

    public Double getOverallAccuracy() {
        return overallAccuracy;
    }

    public void setOverallAccuracy(Double overallAccuracy) {
        this.overallAccuracy = overallAccuracy;
    }

    public Integer getTotalXp() {
        return totalXp;
    }

    public void setTotalXp(Integer totalXp) {
        this.totalXp = totalXp;
    }
}
