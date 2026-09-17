package com.neurolearn.dto;

public class NextQuestionResponse {
    private QuestionResponse question;
    private Integer currentDifficulty;
    private String adaptiveAction;
    private String adaptiveReason;
    private Integer questionIndex;

    public NextQuestionResponse() {}

    public NextQuestionResponse(QuestionResponse question, Integer currentDifficulty, String adaptiveAction, String adaptiveReason, Integer questionIndex) {
        this.question = question;
        this.currentDifficulty = currentDifficulty;
        this.adaptiveAction = adaptiveAction;
        this.adaptiveReason = adaptiveReason;
        this.questionIndex = questionIndex;
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
}
