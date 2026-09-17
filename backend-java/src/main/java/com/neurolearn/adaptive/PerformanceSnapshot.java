package com.neurolearn.adaptive;

public record PerformanceSnapshot(
        double recentAccuracy,
        double overallAccuracy,
        double avgResponseTimeMs,
        int consecutiveCorrect,
        int consecutiveIncorrect,
        int currentDifficulty,
        double topicMastery
) {}
