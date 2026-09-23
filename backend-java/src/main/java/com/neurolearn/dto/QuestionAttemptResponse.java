package com.neurolearn.dto;

public class QuestionAttemptResponse {
    private Long id;
    private Long sessionId;
    private Long questionId;
    private Boolean correct;
    private Long responseTimeMs;
    private Integer xpEarned;
    private Integer totalXp;
    private Integer currentLevel;
    private Double pKnow;
    private Boolean isMastered;
    private String bktExplanation;

    public QuestionAttemptResponse() {}

    public QuestionAttemptResponse(Long id, Long sessionId, Long questionId, Boolean correct, Long responseTimeMs, Integer xpEarned, Integer totalXp, Integer currentLevel) {
        this(id, sessionId, questionId, correct, responseTimeMs, xpEarned, totalXp, currentLevel, 0.20, false, "");
    }

    public QuestionAttemptResponse(Long id, Long sessionId, Long questionId, Boolean correct, Long responseTimeMs, Integer xpEarned, Integer totalXp, Integer currentLevel, Double pKnow, Boolean isMastered, String bktExplanation) {
        this.id = id;
        this.sessionId = sessionId;
        this.questionId = questionId;
        this.correct = correct;
        this.responseTimeMs = responseTimeMs;
        this.xpEarned = xpEarned;
        this.totalXp = totalXp;
        this.currentLevel = currentLevel;
        this.pKnow = pKnow;
        this.isMastered = isMastered;
        this.bktExplanation = bktExplanation;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public Boolean getCorrect() { return correct; }
    public void setCorrect(Boolean correct) { this.correct = correct; }

    public Long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Long responseTimeMs) { this.responseTimeMs = responseTimeMs; }

    public Integer getXpEarned() { return xpEarned; }
    public void setXpEarned(Integer xpEarned) { this.xpEarned = xpEarned; }

    public Integer getTotalXp() { return totalXp; }
    public void setTotalXp(Integer totalXp) { this.totalXp = totalXp; }

    public Integer getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(Integer currentLevel) { this.currentLevel = currentLevel; }

    public Double getPKnow() { return pKnow; }
    public void setPKnow(Double pKnow) { this.pKnow = pKnow; }

    public Boolean getIsMastered() { return isMastered; }
    public void setIsMastered(Boolean isMastered) { this.isMastered = isMastered; }

    public String getBktExplanation() { return bktExplanation; }
    public void setBktExplanation(String bktExplanation) { this.bktExplanation = bktExplanation; }
}
