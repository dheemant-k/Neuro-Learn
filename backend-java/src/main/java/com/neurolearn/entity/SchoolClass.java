package com.neurolearn.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** A teacher's class. Students join it with {@link #joinCode}. ("Class" is reserved in Java.) */
@Entity
@Table(name = "classes")
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_user_id", nullable = false)
    private User teacher;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "join_code", nullable = false, unique = true, length = 12)
    private String joinCode;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected SchoolClass() {
    }

    public SchoolClass(User teacher, String name, String joinCode) {
        this.teacher = teacher;
        this.name = name;
        this.joinCode = joinCode;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public Long getId() { return id; }
    public User getTeacher() { return teacher; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getJoinCode() { return joinCode; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
