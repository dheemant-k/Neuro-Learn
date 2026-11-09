package com.neurolearn.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** A hint/assist a student used on one question in one session (at most one per type). */
@Entity
@Table(name = "session_hint_events", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"session_id", "question_id", "hint_type"})
})
public class SessionHintEvent {

    public enum HintType { FIFTY_FIFTY, CLUE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private LearningSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Enumerated(EnumType.STRING)
    @Column(name = "hint_type", nullable = false, length = 20)
    private HintType hintType;

    /** Comma-separated option letters removed by FIFTY_FIFTY, e.g. "B,D". */
    @Column(name = "eliminated_options", length = 8)
    private String eliminatedOptions;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected SessionHintEvent() {
    }

    public SessionHintEvent(LearningSession session, Question question, HintType hintType, String eliminatedOptions) {
        this.session = session;
        this.question = question;
        this.hintType = hintType;
        this.eliminatedOptions = eliminatedOptions;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public Long getId() { return id; }
    public HintType getHintType() { return hintType; }
    public String getEliminatedOptions() { return eliminatedOptions; }
}
