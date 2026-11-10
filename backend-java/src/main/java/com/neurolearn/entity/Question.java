package com.neurolearn.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(nullable = false)
    private Integer difficulty = 1;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "option_a", nullable = false, columnDefinition = "TEXT")
    private String optionA;

    @Column(name = "option_b", nullable = false, columnDefinition = "TEXT")
    private String optionB;

    @Column(name = "option_c", nullable = false, columnDefinition = "TEXT")
    private String optionC;

    @Column(name = "option_d", nullable = false, columnDefinition = "TEXT")
    private String optionD;

    @Column(name = "correct_answer", nullable = false, length = 10)
    private String correctAnswer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(length = 50)
    private String subject = "Math";

    @Column(name = "character_theme", length = 50)
    private String characterTheme = "batman";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private QuestionStatus status = QuestionStatus.APPROVED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private QuestionSource source = QuestionSource.SEED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(name = "generator_model", length = 60)
    private String generatorModel;

    @Column(name = "prompt_version", length = 20)
    private String promptVersion;

    @Column(name = "created_at", nullable = false)
    private java.time.OffsetDateTime createdAt;

    @Column(name = "reviewed_at")
    private java.time.OffsetDateTime reviewedAt;

    public Question() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = java.time.OffsetDateTime.now();
        }
    }

    public QuestionStatus getStatus() { return status; }
    public void setStatus(QuestionStatus status) { this.status = status; }
    public QuestionSource getSource() { return source; }
    public void setSource(QuestionSource source) { this.source = source; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public String getGeneratorModel() { return generatorModel; }
    public void setGeneratorModel(String generatorModel) { this.generatorModel = generatorModel; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public java.time.OffsetDateTime getCreatedAt() { return createdAt; }
    public java.time.OffsetDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(java.time.OffsetDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public Question(Long id, String topic, Integer difficulty, String questionText, String optionA, String optionB, String optionC, String optionD, String correctAnswer, String explanation, String subject) {
        this(id, topic, difficulty, questionText, optionA, optionB, optionC, optionD, correctAnswer, explanation, subject, "batman");
    }

    public Question(Long id, String topic, Integer difficulty, String questionText, String optionA, String optionB, String optionC, String optionD, String correctAnswer, String explanation, String subject, String characterTheme) {
        this.id = id;
        this.topic = topic;
        this.difficulty = difficulty != null ? difficulty : 1;
        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.subject = subject != null ? subject : "Math";
        this.characterTheme = characterTheme != null ? characterTheme : "batman";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String optionC) {
        this.optionC = optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String optionD) {
        this.optionD = optionD;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCharacterTheme() {
        return characterTheme;
    }

    public void setCharacterTheme(String characterTheme) {
        this.characterTheme = characterTheme;
    }
}
