package com.neurolearn.evaluation;

import com.neurolearn.adaptive.AdaptiveDecision;
import com.neurolearn.adaptive.AdaptiveEngineService;
import com.neurolearn.adaptive.BktParameters;

import java.util.Random;

/** Chooses the difficulty level (1–3) of the next question for one student. */
public interface DifficultyPolicy {

    String name();

    /** Fresh per-student state. */
    Session start(long seed);

    interface Session {
        int nextLevel();

        void observe(int level, boolean correct);

        /** The policy's own belief that the student has mastered the topic, if it has one (else NaN). */
        default double believedMastery() {
            return Double.NaN;
        }

        /** Whether the policy would tell the student "mastered" now (the app's badge uses P(L) ≥ 0.85). */
        default boolean claimsMastery() {
            double b = believedMastery();
            return !Double.isNaN(b) && b >= 0.85;
        }
    }

    /**
     * Difficulty-aware alternative (evaluation only): an Elo-style ability estimate θ̂, updated by
     * how surprising each answer was for that item's difficulty, then the level whose predicted
     * success is closest to {@code targetSuccess}. Claims mastery when predicted level-3 accuracy ≥ 70%.
     *
     * <p><b>Advantage to note:</b> it is given the items' difficulties (as if IRT-calibrated) and the
     * same response-curve shape as the simulator. In the app that calibration needs real data first.
     */
    static DifficultyPolicy eloTarget(double targetSuccess) {
        LearningModel itemModel = LearningModel.standard();
        double k = 0.6;
        String name = String.format(java.util.Locale.ROOT, "elo_target%.0f", targetSuccess * 100);
        return new DifficultyPolicy() {
            public String name() { return name; }

            public Session start(long seed) {
                return new Session() {
                    double thetaHat = 0.0;

                    public int nextLevel() {
                        int best = 1;
                        double bestGap = Double.MAX_VALUE;
                        for (int level = 1; level <= 3; level++) {
                            double gap = Math.abs(itemModel.pCorrect(thetaHat, level) - targetSuccess);
                            if (gap < bestGap) {
                                bestGap = gap;
                                best = level;
                            }
                        }
                        return best;
                    }

                    public void observe(int level, boolean correct) {
                        double expected = itemModel.pCorrect(thetaHat, level);
                        thetaHat += k * ((correct ? 1.0 : 0.0) - expected);
                    }

                    public double believedMastery() { return itemModel.pCorrect(thetaHat, 3); }

                    public boolean claimsMastery() { return believedMastery() >= LearningModel.MASTERY_ACCURACY; }
                };
            }
        };
    }

    /** Textbook-style linear progression: 5 easy, 5 medium, then hard. Ignores answers. */
    static DifficultyPolicy fixed() {
        return new DifficultyPolicy() {
            public String name() { return "fixed"; }

            public Session start(long seed) {
                return new Session() {
                    int asked = 0;
                    public int nextLevel() { return asked < 5 ? 1 : asked < 10 ? 2 : 3; }
                    public void observe(int level, boolean correct) { asked++; }
                };
            }
        };
    }

    /**
     * Fixed ramp with remediation (a teacher's rule of thumb, no tuned parameters): move up a level
     * after 5 questions at a level, but after 2 wrong answers in a row drop back one level and
     * restart the count there.
     */
    static DifficultyPolicy rampWithRemediation() {
        return new DifficultyPolicy() {
            public String name() { return "ramp_remediation"; }

            public Session start(long seed) {
                return new Session() {
                    int level = 1;
                    int atLevel = 0;
                    int wrongStreak = 0;

                    public int nextLevel() { return level; }

                    public void observe(int lvl, boolean correct) {
                        atLevel++;
                        wrongStreak = correct ? 0 : wrongStreak + 1;
                        if (wrongStreak >= 2 && level > 1) {
                            level--;
                            atLevel = 0;
                            wrongStreak = 0;
                        } else if (atLevel >= 5 && level < 3) {
                            level++;
                            atLevel = 0;
                        }
                    }
                };
            }
        };
    }

    /** Uniformly random level — a floor for comparison. */
    static DifficultyPolicy random() {
        return new DifficultyPolicy() {
            public String name() { return "random"; }

            public Session start(long seed) {
                Random rng = new Random(seed ^ 0x5DEECE66DL);
                return new Session() {
                    public int nextLevel() { return 1 + rng.nextInt(3); }
                    public void observe(int level, boolean correct) { }
                };
            }
        };
    }

    /**
     * Experimental variant of the engine (evaluation only — not used by the app):
     * same BKT model, but configurable P(L) cut-offs for levels 2 and 3, and optionally
     * <b>level-aware evidence</b>: a correct answer on an easy item is treated as more guessable
     * (P(G) = 0.60 for level 1, 0.40 for level 2, default 0.25 for level 3), so easy wins move P(L) less.
     */
    static DifficultyPolicy adaptiveVariant(double toLevel2, double toLevel3, boolean levelAwareEvidence) {
        AdaptiveEngineService engine = new AdaptiveEngineService(BktParameters.defaultParameters());
        double[] guessByLevel = {Double.NaN, 0.60, 0.40, engine.getParameters().pGuess()};
        String name = String.format(java.util.Locale.ROOT, "adaptive_%.2f_%.2f%s", toLevel2, toLevel3, levelAwareEvidence ? "_levelaware" : "");
        return new DifficultyPolicy() {
            public String name() { return name; }

            public Session start(long seed) {
                return new Session() {
                    double pKnow = engine.getParameters().pInit();
                    int asked = 0;

                    public int nextLevel() {
                        if (asked == 0) return 1;
                        return pKnow < toLevel2 ? 1 : pKnow < toLevel3 ? 2 : 3;
                    }

                    public void observe(int level, boolean correct) {
                        pKnow = levelAwareEvidence
                                ? engine.updateKnowledge(pKnow, correct, guessByLevel[level]).updatedPKnow()
                                : engine.updateKnowledge(pKnow, correct).updatedPKnow();
                        asked++;
                    }

                    public double believedMastery() { return pKnow; }
                };
            }
        };
    }

    /**
     * Neuro-Learn's real engine, unchanged: BKT update after each answer
     * ({@link AdaptiveEngineService#updateKnowledge}) and the same difficulty rule the app uses
     * ({@link AdaptiveEngineService#evaluateBkt}).
     */
    static DifficultyPolicy adaptiveEngine() {
        AdaptiveEngineService engine = new AdaptiveEngineService(BktParameters.defaultParameters());
        return new DifficultyPolicy() {
            public String name() { return "adaptive"; }

            public Session start(long seed) {
                return new Session() {
                    double pKnow = engine.getParameters().pInit();
                    int lastLevel = 1;
                    int asked = 0;

                    public int nextLevel() {
                        AdaptiveDecision d = engine.evaluateBkt(pKnow, lastLevel, asked);
                        return d.nextDifficulty();
                    }

                    public void observe(int level, boolean correct) {
                        pKnow = engine.updateKnowledge(pKnow, correct).updatedPKnow();
                        lastLevel = level;
                        asked++;
                    }

                    public double believedMastery() { return pKnow; }
                };
            }
        };
    }
}
