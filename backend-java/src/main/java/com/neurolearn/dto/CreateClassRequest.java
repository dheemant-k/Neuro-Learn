package com.neurolearn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClassRequest(
        @NotBlank(message = "is required") @Size(max = 100, message = "must be at most 100 characters") String name
) {
}
