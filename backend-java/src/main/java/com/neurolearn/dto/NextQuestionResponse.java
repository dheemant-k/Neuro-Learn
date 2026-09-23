package com.neurolearn.dto;

public class NextQuestionResponse {
    private QuestionResponse question;
    private Integer currentDifficulty;
    private String adaptiveAction;
    private String adaptiveReason;
    private Integer questionIndex;
    private Double pKnow;
    private Double predictedCorrectProb;
    private Boolean isMastered;

    public NextQuestionResponse() {}

    public NextQuestionResponse(QuestionResponse question, Integer currentDifficulty, String adaptiveAction, String adaptiveReason, Integer questionIndex) {
        this(question, currentDifficulty, adaptiveAction, adaptiveReason, questionIndex, 0.20, 0.38, false);
    }

    public NextQuestionResponse(QuestionResponse question, Integer currentDifficulty, String adaptiveAction, String adaptiveReason, Integer questionIndex, Double pKnow, Double predictedCorrectProb, Boolean isMastered) {
        this.question = question;
        this.currentDifficulty = currentDifficulty;
        this.adaptiveAction = adaptiveAction;
        this.adaptiveReason = adaptiveReason;
        this.questionIndex = questionIndex;
        this.pKnow = pKnow;
        this.predictedCorrectProb = predictedCorrectProb;
        this.isMastered = isMastered;
    }

    public QuestionResponse getQuestion() { return question; }
    public void setQuestion(QuestionResponse question) { this.question = question; }

    public Integer getCurrentDifficulty() { return currentDifficulty; }
    public void setCurrentDifficulty(Integer currentDifficulty) { this.currentDifficulty = currentDifficulty; }

    public String getAdaptiveAction() { return adaptiveAction; }
    public void setAdaptiveAction(String adaptiveAction) { this.adaptiveAction = adaptiveAction; }

    public String getAdaptiveReason() { return adaptiveReason; }
    public void setAdaptiveReason(String adaptiveReason) { this.adaptiveReason = adaptiveReason; }

    public Integer getQuestionIndex() { return questionIndex; }
    public void setQuestionIndex(Integer questionIndex) { this.questionIndex = questionIndex; }

    public Double getPKnow() { return pKnow; }
    public void setPKnow(Double pKnow) { this.pKnow = pKnow; }

    public Double getPredictedCorrectProb() { return predictedCorrectProb; }
    public void setPredictedCorrectProb(Double predictedCorrectProb) { this.predictedCorrectProb = predictedCorrectProb; }

    public Boolean getIsMastered() { return isMastered; }
    public void setIsMastered(Boolean isMastered) { this.isMastered = isMastered; }
}
