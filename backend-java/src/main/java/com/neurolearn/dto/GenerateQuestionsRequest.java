package com.neurolearn.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Teacher asks for 1–5 new questions for one section, difficulty and character theme. */
public record GenerateQuestionsRequest(
        @NotBlank(message = "is required") String topic,
        @NotNull(message = "is required") @Min(value = 1, message = "must be 1-3") @Max(value = 3, message = "must be 1-3") Integer difficulty,
        @NotBlank(message = "is required") String characterTheme,
        @NotNull(message = "is required") @Min(value = 1, message = "must be 1-5") @Max(value = 5, message = "must be 1-5") Integer count
) {
}
