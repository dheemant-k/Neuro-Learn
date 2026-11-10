package com.neurolearn.entity;

import jakarta.persistence.*;

/** The student mistake a wrong option represents, e.g. "added the equations instead of subtracting". */
@Entity
@Table(name = "question_misconceptions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"question_id", "option_key"})
})
public class QuestionMisconception {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "option_key", nullable = false, length = 1)
    private String optionKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String misconception;

    protected QuestionMisconception() {
    }

    public QuestionMisconception(Question question, String optionKey, String misconception) {
        this.question = question;
        this.optionKey = optionKey;
        this.misconception = misconception;
    }

    public Long getId() { return id; }
    public Question getQuestion() { return question; }
    public String getOptionKey() { return optionKey; }
    public String getMisconception() { return misconception; }
    public void setMisconception(String misconception) { this.misconception = misconception; }
}
