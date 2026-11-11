package com.neurolearn.dto;

import java.util.Map;

/** Teacher edits to a PENDING question before approving it. All fields are re-validated. */
public record UpdateQuestionRequest(
        String questionText,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        String correctAnswer,
        String explanation,
        Integer difficulty,
        Map<String, String> misconceptions
) {
}
