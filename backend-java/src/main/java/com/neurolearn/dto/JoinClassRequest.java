package com.neurolearn.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinClassRequest(@NotBlank(message = "is required") String joinCode) {
}
