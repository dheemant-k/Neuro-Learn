package com.neurolearn.dto;

public class QuestionAttemptRequest {
    private Long questionId;
    private Boolean correct;
    private Long responseTimeMs;
    private Integer attemptNumber;

    public QuestionAttemptRequest() {}

    public QuestionAttemptRequest(Long questionId, Boolean correct, Long responseTimeMs, Integer attemptNumber) {
        this.questionId = questionId;
        this.correct = correct;
        this.responseTimeMs = responseTimeMs;
        this.attemptNumber = attemptNumber;
    }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public Boolean getCorrect() { return correct; }
    public void setCorrect(Boolean correct) { this.correct = correct; }

    public Long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Long responseTimeMs) { this.responseTimeMs = responseTimeMs; }

    public Integer getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(Integer attemptNumber) { this.attemptNumber = attemptNumber; }
}
