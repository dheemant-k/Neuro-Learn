package com.neurolearn.dto;

import java.util.List;

/** created = saved as PENDING for review; rejected = dropped by validation or the self-check, with why. */
public record GenerationResultResponse(
        List<TeacherQuestionResponse> created,
        List<RejectedCandidate> rejected,
        String model
) {
    public record RejectedCandidate(String questionText, String reason) {
    }
}
