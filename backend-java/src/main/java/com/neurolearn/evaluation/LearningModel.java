package com.neurolearn.evaluation;

/**
 * Ground-truth model of a simulated student (the thing the adaptive engine cannot see).
 *
 * <p>Assumptions — stated so results can be judged, and varied in sensitivity runs:
 * <ul>
 *   <li><b>Answering</b>: ability θ (logits) vs item difficulty b, with a 4-option guess floor and a small slip:
 *       P(correct) = c + (1 − c − s) · σ(a·(θ − b)).</li>
 *   <li><b>Learning</b>: every attempt (with feedback) raises θ by η · exp(−((b − θ) − δ)² / 2w²). Learning peaks
 *       when the item is δ logits <i>above</i> the student (zone of proximal development); far-too-easy or
 *       far-too-hard items teach little.</li>
 *   <li><b>True mastery</b>: expected accuracy on level-3 items ≥ {@link #MASTERY_ACCURACY}.</li>
 * </ul>
 */
public record LearningModel(
        double guess,          // c: chance of guessing right on a 4-option MCQ
        double slip,           // s: chance a knowing student still answers wrong
        double discrimination, // a
        double zpdOffset,      // δ: how far above the student an item teaches best
        double zpdWidth,       // w
        double quitPerWrongStreak // chance of quitting after each answer = this x consecutive wrong answers (0 = never quits)
) {
    /** Item difficulty (logits) for the app's levels 1, 2, 3. */
    static final double[] LEVEL_DIFFICULTY = {Double.NaN, -1.0, 0.5, 2.0};
    static final double MASTERY_ACCURACY = 0.70;

    public static LearningModel standard() {
        return new LearningModel(0.25, 0.05, 1.2, 0.5, 1.0, 0.0);
    }

    public LearningModel withZpdOffset(double delta) {
        return new LearningModel(guess, slip, discrimination, delta, zpdWidth, quitPerWrongStreak);
    }

    /** Frustration: after k wrong answers in a row the student quits with probability k * rate. */
    public LearningModel withQuitRate(double rate) {
        return new LearningModel(guess, slip, discrimination, zpdOffset, zpdWidth, rate);
    }

    public double pCorrect(double theta, int level) {
        double b = LEVEL_DIFFICULTY[level];
        return guess + (1.0 - guess - slip) * sigmoid(discrimination * (theta - b));
    }

    public double learningGain(double theta, int level, double eta) {
        double gap = LEVEL_DIFFICULTY[level] - theta;
        double z = (gap - zpdOffset) / zpdWidth;
        return eta * Math.exp(-0.5 * z * z);
    }

    public double quitProbability(int wrongStreak) {
        return Math.min(1.0, quitPerWrongStreak * wrongStreak);
    }

    public boolean isMastered(double theta) {
        return pCorrect(theta, 3) >= MASTERY_ACCURACY;
    }

    static double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }
}
