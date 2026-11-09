package com.neurolearn.dto;

/**
 * A student's answer. The server grades it from {@code selectedOption};
 * the client no longer tells the server whether it was correct.
 */
public class QuestionAttemptRequest {
    private Long questionId;
    /** "A", "B", "C" or "D" (case-insensitive). */
    private String selectedOption;
    private Long responseTimeMs;
    private Integer attemptNumber;

    public QuestionAttemptRequest() {}

    public QuestionAttemptRequest(Long questionId, String selectedOption, Long responseTimeMs, Integer attemptNumber) {
        this.questionId = questionId;
        this.selectedOption = selectedOption;
        this.responseTimeMs = responseTimeMs;
        this.attemptNumber = attemptNumber;
    }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public String getSelectedOption() { return selectedOption; }
    public void setSelectedOption(String selectedOption) { this.selectedOption = selectedOption; }
    public Long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Long responseTimeMs) { this.responseTimeMs = responseTimeMs; }
    public Integer getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(Integer attemptNumber) { this.attemptNumber = attemptNumber; }
}
