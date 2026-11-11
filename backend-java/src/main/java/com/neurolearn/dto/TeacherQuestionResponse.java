package com.neurolearn.dto;

import java.time.OffsetDateTime;
import java.util.Map;

/** A question as its teacher reviews it: includes the answer, explanation and per-option misconceptions. */
public record TeacherQuestionResponse(
        Long id,
        String topic,
        Integer difficulty,
        String characterTheme,
        String questionText,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        String correctAnswer,
        String explanation,
        Map<String, String> misconceptions,
        String status,
        String source,
        String generatorModel,
        OffsetDateTime createdAt
) {
}
