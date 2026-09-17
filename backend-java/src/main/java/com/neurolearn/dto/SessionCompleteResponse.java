package com.neurolearn.dto;

public class SessionCompleteResponse {
    private Long sessionId;
    private Integer totalQuestions;
    private Integer totalCorrect;
    private Double accuracy;
    private Integer totalXp;
    private Integer currentLevel;
    private Double topicMastery;

    public SessionCompleteResponse() {}

    public SessionCompleteResponse(Long sessionId, Integer totalQuestions, Integer totalCorrect, Double accuracy, Integer totalXp, Integer currentLevel, Double topicMastery) {
        this.sessionId = sessionId;
        this.totalQuestions = totalQuestions;
        this.totalCorrect = totalCorrect;
        this.accuracy = accuracy;
        this.totalXp = totalXp;
        this.currentLevel = currentLevel;
        this.topicMastery = topicMastery;
    }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public Integer getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; }

    public Integer getTotalCorrect() { return totalCorrect; }
    public void setTotalCorrect(Integer totalCorrect) { this.totalCorrect = totalCorrect; }

    public Double getAccuracy() { return accuracy; }
    public void setAccuracy(Double accuracy) { this.accuracy = accuracy; }

    public Integer getTotalXp() { return totalXp; }
    public void setTotalXp(Integer totalXp) { this.totalXp = totalXp; }

    public Integer getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(Integer currentLevel) { this.currentLevel = currentLevel; }

    public Double getTopicMastery() { return topicMastery; }
    public void setTopicMastery(Double topicMastery) { this.topicMastery = topicMastery; }
}
