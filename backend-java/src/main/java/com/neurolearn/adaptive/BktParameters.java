package com.neurolearn.adaptive;

public record BktParameters(
        double pInit,
        double pTransit,
        double pGuess,
        double pSlip,
        double masteryThreshold
) {
    public static BktParameters defaultParameters() {
        return new BktParameters(0.20, 0.18, 0.25, 0.10, 0.85);
    }
}
