package com.neurolearn.adaptive;

import org.springframework.stereotype.Service;

@Service
public class AdaptiveEngineService {

    private final BktParameters parameters;

    public AdaptiveEngineService() {
        this.parameters = BktParameters.defaultParameters();
    }

    public AdaptiveEngineService(BktParameters parameters) {
        this.parameters = parameters;
    }

    public BktParameters getParameters() {
        return parameters;
    }

    /**
     * Executes the standard Corbett & Anderson (1995) Bayesian Knowledge Tracing update.
     */
    public BktUpdateResult updateKnowledge(double priorPKnow, boolean isCorrect) {
        // Clamp prior within reasonable bounds
        double pL = Math.max(0.01, Math.min(0.99, priorPKnow));
        double pS = parameters.pSlip();
        double pG = parameters.pGuess();
        double pT = parameters.pTransit();

        double posterior;
        if (isCorrect) {
            double numerator = pL * (1.0 - pS);
            double denominator = numerator + ((1.0 - pL) * pG);
            posterior = numerator / denominator;
        } else {
            double numerator = pL * pS;
            double denominator = numerator + ((1.0 - pL) * (1.0 - pG));
            posterior = numerator / denominator;
        }

        // Apply learning transition opportunity
        double updatedPKnow = posterior + ((1.0 - posterior) * pT);
        updatedPKnow = Math.max(0.01, Math.min(0.99, updatedPKnow));

        // Predicted probability of answering the NEXT question correctly
        double predictedCorrectProb = (updatedPKnow * (1.0 - pS)) + ((1.0 - updatedPKnow) * pG);
        boolean isMastered = updatedPKnow >= parameters.masteryThreshold();

        int priorPct = (int) Math.round(pL * 100);
        int updatedPct = (int) Math.round(updatedPKnow * 100);
        int delta = updatedPct - priorPct;
        String sign = delta >= 0 ? "+" : "";

        String explanation;
        if (isCorrect) {
            explanation = String.format(
                    "Bayesian Evidence: Correct answer (P(G)=%.2f, P(S)=%.2f). Knowledge belief: %d%% -> %d%% (%s%d%%). Next solve prob: %.0f%%.",
                    pG, pS, priorPct, updatedPct, sign, delta, predictedCorrectProb * 100
            );
        } else {
            explanation = String.format(
                    "Bayesian Evidence: Incorrect answer (P(G)=%.2f, P(S)=%.2f). Knowledge belief: %d%% -> %d%% (%s%d%%). Next solve prob: %.0f%%.",
                    pG, pS, priorPct, updatedPct, sign, delta, predictedCorrectProb * 100
            );
        }

        return new BktUpdateResult(
                roundTwoDecimals(pL),
                roundTwoDecimals(posterior),
                roundTwoDecimals(updatedPKnow),
                roundTwoDecimals(predictedCorrectProb),
                isMastered,
                explanation
        );
    }

    /**
     * Determines adaptive difficulty based on the Bayesian Knowledge state P(L).
     */
    public AdaptiveDecision evaluateBkt(double pKnow, int lastDifficulty, int sessionAttemptCount) {
        double safeP = Math.max(0.01, Math.min(0.99, pKnow));
        double nextProb = (safeP * (1.0 - parameters.pSlip())) + ((1.0 - safeP) * parameters.pGuess());
        boolean isMastered = safeP >= parameters.masteryThreshold();
        int pPct = (int) Math.round(safeP * 100);

        if (sessionAttemptCount == 0) {
            return new AdaptiveDecision(
                    DifficultyAction.MAINTAIN,
                    1,
                    String.format("Initial Diagnostic: Baseline knowledge state P(L)=%d%%. Serving foundational Level 1 question.", pPct),
                    safeP,
                    nextProb,
                    isMastered
            );
        }

        int targetDifficulty;
        DifficultyAction action;
        String reason;

        if (safeP < 0.40) {
            targetDifficulty = 1;
            action = (lastDifficulty > 1) ? DifficultyAction.DECREASE : DifficultyAction.MAINTAIN;
            reason = String.format(
                    "Knowledge State P(L)=%d%% (<40%%). %s Level 1 foundational questions to build core comprehension.",
                    pPct, action == DifficultyAction.DECREASE ? "Calibrating down to" : "Maintaining"
            );
        } else if (safeP < 0.75) {
            targetDifficulty = 2;
            if (lastDifficulty < 2) {
                action = DifficultyAction.INCREASE;
                reason = String.format("Knowledge State P(L)=%d%% (40-74%%). Advanced to Level 2 intermediate application.", pPct);
            } else if (lastDifficulty > 2) {
                action = DifficultyAction.DECREASE;
                reason = String.format("Knowledge State P(L)=%d%% (40-74%%). Adjusting to Level 2 for balanced practice.", pPct);
            } else {
                action = DifficultyAction.MAINTAIN;
                reason = String.format("Knowledge State P(L)=%d%% (40-74%%). Maintaining Level 2 optimal challenge zone.", pPct);
            }
        } else {
            targetDifficulty = 3;
            action = (lastDifficulty < 3) ? DifficultyAction.INCREASE : DifficultyAction.MAINTAIN;
            if (isMastered) {
                reason = String.format("Topic Mastered! P(L)=%d%% (>=85%%). Challenging with Level 3 complex synthesis questions.", pPct);
            } else {
                reason = String.format("High Knowledge P(L)=%d%% (>=75%%). Advanced to Level 3 advanced multi-step problems.", pPct);
            }
        }

        return new AdaptiveDecision(action, targetDifficulty, reason, safeP, nextProb, isMastered);
    }

    /**
     * Backward-compatible fallback.
     */
    public AdaptiveDecision evaluate(PerformanceSnapshot snapshot) {
        return evaluateBkt(snapshot.topicMastery(), snapshot.currentDifficulty(), snapshot.consecutiveCorrect() + snapshot.consecutiveIncorrect());
    }

    private double roundTwoDecimals(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
