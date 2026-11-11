package com.neurolearn.evaluation;

import java.util.Arrays;
import java.util.List;

/** Aggregate outcome of one policy over a population of simulated students. */
public record PolicySummary(
        String scenario,
        String policy,
        int students,
        int horizon,
        double masteredBy10,
        double masteredBy20,
        double masteredByEnd,
        double medianQuestionsToMastery,   // among students who reached mastery
        double meanFinalTheta,
        double meanThetaGain,
        double observedAccuracy,
        double shareProductive,            // items with P(correct) in [0.50, 0.85]
        double shareTooHard,               // P(correct) < 0.50
        double shareTooEasy,               // P(correct) > 0.85
        double engineClaimPrecision,       // when the engine first says "mastered", how often truly mastered (NaN if no belief)
        double engineClaimRecall,          // of truly-mastered-at-end students, share the engine also called mastered
        double medianEngineClaimStep,      // question number when the engine first says "mastered" (median, NaN if none)
        double quitRate                    // share of students who quit before the end (frustration scenario)
) {
    static final double PRODUCTIVE_LOW = 0.50;
    static final double PRODUCTIVE_HIGH = 0.85;

    public static PolicySummary of(String scenario, String policy, List<Simulator.Run> runs) {
        int n = runs.size();
        int horizon = runs.get(0).steps().size();
        int m10 = 0, m20 = 0, mEnd = 0;
        double[] toMastery = new double[n];
        int reached = 0;
        double finalTheta = 0, gain = 0, correct = 0, productive = 0, hard = 0, easy = 0, items = 0;
        int claims = 0, claimsTrue = 0, trulyAtEnd = 0, trulyAndClaimed = 0;
        double[] claimSteps = new double[n];
        int quitters = 0;
        boolean hasBelief = false;

        for (Simulator.Run r : runs) {
            List<Simulator.Step> steps = r.steps();
            int first = -1;
            int firstClaim = -1;
            if (!steps.get(steps.size() - 1).active()) quitters++;
            for (Simulator.Step s : steps) {
                if (first < 0 && s.trulyMastered()) first = s.index();
                if (!s.active()) continue;
                if (!Double.isNaN(s.believedMastery())) {
                    hasBelief = true;
                    if (firstClaim < 0 && s.claimedMastery()) {
                        firstClaim = s.index();
                        claimSteps[claims] = firstClaim;
                        claims++;
                        if (s.trulyMastered()) claimsTrue++;
                    }
                }
                items++;
                if (s.correct()) correct++;
                if (s.pCorrect() < PRODUCTIVE_LOW) hard++;
                else if (s.pCorrect() > PRODUCTIVE_HIGH) easy++;
                else productive++;
            }
            if (first > 0) {
                toMastery[reached++] = first;
                if (first <= 10) m10++;
                if (first <= 20) m20++;
                mEnd++;
            }
            Simulator.Step last = steps.get(steps.size() - 1);
            finalTheta += last.thetaAfter();
            gain += last.thetaAfter() - r.student().theta0();
            if (last.trulyMastered()) {
                trulyAtEnd++;
                if (firstClaim > 0) trulyAndClaimed++;
            }
        }
        double median = Double.NaN;
        if (reached > 0) {
            double[] sorted = Arrays.copyOf(toMastery, reached);
            Arrays.sort(sorted);
            median = reached % 2 == 1 ? sorted[reached / 2] : (sorted[reached / 2 - 1] + sorted[reached / 2]) / 2.0;
        }
        return new PolicySummary(scenario, policy, n, horizon,
                m10 / (double) n, m20 / (double) n, mEnd / (double) n, median,
                finalTheta / n, gain / n, correct / items,
                productive / items, hard / items, easy / items,
                hasBelief && claims > 0 ? claimsTrue / (double) claims : Double.NaN,
                hasBelief && trulyAtEnd > 0 ? trulyAndClaimed / (double) trulyAtEnd : Double.NaN,
                median(claimSteps, claims),
                quitters / (double) n);
    }

    private static double median(double[] values, int count) {
        if (count == 0) return Double.NaN;
        double[] sorted = Arrays.copyOf(values, count);
        Arrays.sort(sorted);
        return count % 2 == 1 ? sorted[count / 2] : (sorted[count / 2 - 1] + sorted[count / 2]) / 2.0;
    }

    static String csvHeader() {
        return "scenario,policy,students,horizon,mastered_by_10,mastered_by_20,mastered_by_end,median_questions_to_mastery,"
                + "mean_final_theta,mean_theta_gain,observed_accuracy,share_productive,share_too_hard,share_too_easy,"
                + "engine_claim_precision,engine_claim_recall,median_engine_claim_step,quit_rate";
    }

    String toCsv() {
        return String.join(",", scenario, policy, String.valueOf(students), String.valueOf(horizon),
                f(masteredBy10), f(masteredBy20), f(masteredByEnd), f(medianQuestionsToMastery),
                f(meanFinalTheta), f(meanThetaGain), f(observedAccuracy),
                f(shareProductive), f(shareTooHard), f(shareTooEasy),
                f(engineClaimPrecision), f(engineClaimRecall), f(medianEngineClaimStep), f(quitRate));
    }

    static String f(double v) {
        return Double.isNaN(v) ? "" : String.format(java.util.Locale.ROOT, "%.4f", v);
    }
}
