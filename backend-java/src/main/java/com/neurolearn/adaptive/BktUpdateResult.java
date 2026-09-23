package com.neurolearn.adaptive;

public record BktUpdateResult(
        double priorPKnow,
        double posteriorPKnow,
        double updatedPKnow,
        double predictedCorrectProb,
        boolean isMastered,
        String explanation
) {}
