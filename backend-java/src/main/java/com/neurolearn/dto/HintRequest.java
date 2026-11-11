package com.neurolearn.dto;

import jakarta.validation.constraints.NotNull;

/** type: "FIFTY_FIFTY" (removes two wrong options) or "CLUE" (topic clue shown in the UI). */
public record HintRequest(@NotNull(message = "is required") Long questionId,
                          @NotNull(message = "is required") String type) {
}
