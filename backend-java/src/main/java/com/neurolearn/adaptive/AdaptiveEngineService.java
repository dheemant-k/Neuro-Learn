package com.neurolearn.adaptive;

import org.springframework.stereotype.Service;

@Service
public class AdaptiveEngineService {

    private static final int MIN_DIFFICULTY = 1;
    private static final int MAX_DIFFICULTY = 3;
    private static final long FAST_RESPONSE_THRESHOLD_MS = 15000; // 15 seconds

    public AdaptiveDecision evaluate(PerformanceSnapshot snapshot) {
        int currentDiff = snapshot.currentDifficulty();

        // Rule 1: High mastery / high streak -> INCREASE
        if ((snapshot.recentAccuracy() >= 0.80 || snapshot.consecutiveCorrect() >= 2)
                && snapshot.avgResponseTimeMs() <= FAST_RESPONSE_THRESHOLD_MS) {
            int next = Math.min(MAX_DIFFICULTY, currentDiff + 1);
            if (next > currentDiff) {
                return new AdaptiveDecision(
                        DifficultyAction.INCREASE,
                        next,
                        String.format("Advanced to Level %d: High streak (%d correct) & rapid response speed (%.1fs).",
                                next, snapshot.consecutiveCorrect(), snapshot.avgResponseTimeMs() / 1000.0)
                );
            } else {
                return new AdaptiveDecision(
                        DifficultyAction.MAINTAIN,
                        currentDiff,
                        "Max difficulty reached! Demonstrating consistent mastery at Level 3."
                );
            }
        }

        // Rule 2: Struggling (accuracy < 50% or 2 consecutive incorrect) -> DECREASE
        if (snapshot.recentAccuracy() < 0.50 || snapshot.consecutiveIncorrect() >= 2) {
            int next = Math.max(MIN_DIFFICULTY, currentDiff - 1);
            if (next < currentDiff) {
                return new AdaptiveDecision(
                        DifficultyAction.DECREASE,
                        next,
                        String.format("Adjusted to Level %d: Recalibrating to reinforce linear equation fundamentals.", next)
                );
            } else {
                return new AdaptiveDecision(
                        DifficultyAction.MAINTAIN,
                        currentDiff,
                        "Maintaining Level 1 foundation questions to build student confidence."
                );
            }
        }

        // Rule 3: Sweet spot (50% - 79%) -> MAINTAIN
        return new AdaptiveDecision(
                DifficultyAction.MAINTAIN,
                currentDiff,
                String.format("Maintaining Level %d: Optimal challenge zone (recent accuracy: %.0f%%).",
                        currentDiff, snapshot.recentAccuracy() * 100.0)
        );
    }
}
