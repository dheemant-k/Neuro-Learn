package com.neurolearn.adaptive;

public record AdaptiveDecision(
        DifficultyAction action,
        int nextDifficulty,
        String reason,
        double pKnow,
        double predictedCorrectProb,
        boolean isMastered
) {
    public AdaptiveDecision(DifficultyAction action, int nextDifficulty, String reason) {
        this(action, nextDifficulty, reason, 0.20, 0.38, false);
    }
}
