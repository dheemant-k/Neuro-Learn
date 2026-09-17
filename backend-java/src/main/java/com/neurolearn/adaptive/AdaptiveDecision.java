package com.neurolearn.adaptive;

public record AdaptiveDecision(
        DifficultyAction action,
        int nextDifficulty,
        String reason
) {}
