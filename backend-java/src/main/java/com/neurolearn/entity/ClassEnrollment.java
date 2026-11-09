package com.neurolearn.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "class_enrollments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"class_id", "student_profile_id"})
})
public class ClassEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    protected ClassEnrollment() {
    }

    public ClassEnrollment(SchoolClass schoolClass, StudentProfile studentProfile) {
        this.schoolClass = schoolClass;
        this.studentProfile = studentProfile;
    }

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = OffsetDateTime.now();
        }
    }

    public Long getId() { return id; }
    public SchoolClass getSchoolClass() { return schoolClass; }
    public StudentProfile getStudentProfile() { return studentProfile; }
    public OffsetDateTime getJoinedAt() { return joinedAt; }
}
