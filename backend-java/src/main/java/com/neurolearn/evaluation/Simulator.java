package com.neurolearn.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Runs simulated students through a difficulty policy.
 * Each student is replayed under every policy with the <i>same</i> starting ability,
 * learning rate and random draws (common random numbers), so differences between
 * policies come from the policy, not from luck.
 */
public final class Simulator {

    /** A simulated student: starting ability θ₀ and learning rate η. */
    public record Student(long id, double theta0, double eta) {
    }

    /**
     * One question slot. {@code active=false} means the student had already quit (frustration
     * scenario): nothing was asked, ability is frozen.
     */
    public record Step(int index, int level, double pCorrect, boolean correct, double thetaAfter,
                       boolean trulyMastered, double believedMastery, boolean claimedMastery, boolean active) {
    }

    public record Run(Student student, String policy, List<Step> steps) {
    }

    private final LearningModel model;

    public Simulator(LearningModel model) {
        this.model = model;
    }

    /** Population: θ₀ ~ N(mean, 0.8), η ~ U(0.10, 0.30). */
    public static List<Student> population(int n, double meanTheta0, long seed) {
        Random rng = new Random(seed);
        List<Student> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double theta0 = meanTheta0 + 0.8 * rng.nextGaussian();
            double eta = 0.10 + 0.20 * rng.nextDouble();
            out.add(new Student(i, theta0, eta));
        }
        return out;
    }

    public Run run(Student s, DifficultyPolicy policy, int horizon) {
        DifficultyPolicy.Session session = policy.start(s.id());
        Random answers = new Random(s.id() * 7919L + 17); // same draws for every policy
        Random quits = new Random(s.id() * 104729L + 3);   // same quit draws for every policy
        double theta = s.theta0();
        int wrongStreak = 0;
        boolean quit = false;
        List<Step> steps = new ArrayList<>(horizon);
        for (int i = 0; i < horizon; i++) {
            if (quit) {
                steps.add(new Step(i + 1, 0, Double.NaN, false, theta, model.isMastered(theta),
                        session.believedMastery(), session.claimsMastery(), false));
                continue;
            }
            int level = session.nextLevel();
            double p = model.pCorrect(theta, level);
            boolean correct = answers.nextDouble() < p;
            theta += model.learningGain(theta, level, s.eta());
            session.observe(level, correct);
            steps.add(new Step(i + 1, level, p, correct, theta, model.isMastered(theta),
                    session.believedMastery(), session.claimsMastery(), true));
            wrongStreak = correct ? 0 : wrongStreak + 1;
            if (!model.isMastered(theta) && quits.nextDouble() < model.quitProbability(wrongStreak)) {
                quit = true;
            }
        }
        return new Run(s, policy.name(), steps);
    }
}
