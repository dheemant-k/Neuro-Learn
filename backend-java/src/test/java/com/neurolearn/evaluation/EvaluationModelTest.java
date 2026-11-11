package com.neurolearn.evaluation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Learning model, policies and simulator behave as documented in docs/evaluation.md. */
class EvaluationModelTest {

    private final LearningModel model = LearningModel.standard();

    // ── LearningModel ─────────────────────────────────────────────

    @Test
    void answerProbabilityHasGuessFloorSlipCeilingAndOrdering() {
        assertThat(model.pCorrect(-10, 3)).isCloseTo(0.25, within(1e-3));     // pure guessing
        assertThat(model.pCorrect(+10, 1)).isCloseTo(0.95, within(1e-3));     // 1 - slip
        assertThat(model.pCorrect(0, 1)).isGreaterThan(model.pCorrect(0, 2));
        assertThat(model.pCorrect(0, 2)).isGreaterThan(model.pCorrect(0, 3));
        assertThat(model.pCorrect(1, 2)).isGreaterThan(model.pCorrect(0, 2));
    }

    @Test
    void learningPeaksWhenItemIsSlightlyAboveTheStudent() {
        double eta = 0.2;
        double thetaForPeakOnLevel2 = LearningModel.LEVEL_DIFFICULTY[2] - model.zpdOffset();
        assertThat(model.learningGain(thetaForPeakOnLevel2, 2, eta)).isCloseTo(eta, within(1e-9));
        assertThat(model.learningGain(thetaForPeakOnLevel2 - 3, 2, eta)).isLessThan(0.05 * eta); // far too hard
        assertThat(model.learningGain(thetaForPeakOnLevel2 + 3, 2, eta)).isLessThan(0.05 * eta); // far too easy
    }

    @Test
    void masteryMeans70PercentOnHardestItems() {
        assertThat(model.isMastered(0.0)).isFalse();
        assertThat(model.isMastered(4.0)).isTrue();
        assertThat(model.pCorrect(4.0, 3)).isGreaterThanOrEqualTo(LearningModel.MASTERY_ACCURACY);
    }

    @Test
    void quitProbabilityGrowsWithWrongStreakAndIsZeroByDefault() {
        assertThat(model.quitProbability(5)).isZero();
        LearningModel frustrated = model.withQuitRate(0.04);
        assertThat(frustrated.quitProbability(0)).isZero();
        assertThat(frustrated.quitProbability(3)).isCloseTo(0.12, within(1e-12));
        assertThat(frustrated.quitProbability(100)).isEqualTo(1.0);
    }

    // ── Policies ──────────────────────────────────────────────────

    private static List<Integer> levels(DifficultyPolicy policy, boolean... answers) {
        DifficultyPolicy.Session s = policy.start(1);
        List<Integer> out = new ArrayList<>();
        for (boolean correct : answers) {
            int level = s.nextLevel();
            out.add(level);
            s.observe(level, correct);
        }
        return out;
    }

    private static boolean[] repeat(boolean value, int n) {
        boolean[] a = new boolean[n];
        java.util.Arrays.fill(a, value);
        return a;
    }

    @Test
    void fixedRampIgnoresAnswers() {
        assertThat(levels(DifficultyPolicy.fixed(), repeat(false, 12)))
                .containsExactly(1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3);
    }

    @Test
    void rampWithRemediationDropsBackAfterTwoWrongInARow() {
        boolean[] answers = {true, true, true, true, true, false, false, true, true};
        assertThat(levels(DifficultyPolicy.rampWithRemediation(), answers))
                .containsExactly(1, 1, 1, 1, 1, 2, 2, 1, 1);
    }

    @Test
    void adaptiveEnginePolicyUsesTheAppsRule() {
        // First question is always level 1; a run of correct answers raises P(L) and the level.
        List<Integer> climbing = levels(DifficultyPolicy.adaptiveEngine(), repeat(true, 8));
        assertThat(climbing.get(0)).isEqualTo(1);
        assertThat(climbing.get(climbing.size() - 1)).isEqualTo(3);
        // All wrong keeps it on level 1.
        assertThat(levels(DifficultyPolicy.adaptiveEngine(), repeat(false, 6))).containsOnly(1);
    }

    @Test
    void levelAwareEvidenceMakesEasyWinsCountLess() {
        DifficultyPolicy.Session plain = DifficultyPolicy.adaptiveVariant(0.4, 0.75, false).start(1);
        DifficultyPolicy.Session aware = DifficultyPolicy.adaptiveVariant(0.4, 0.75, true).start(1);
        plain.observe(1, true);
        aware.observe(1, true);
        assertThat(aware.believedMastery()).isLessThan(plain.believedMastery());
    }

    @Test
    void eloEstimateRisesWithCorrectAnswersAndChoosesHarderItems() {
        DifficultyPolicy.Session s = DifficultyPolicy.eloTarget(0.5).start(1);
        double before = s.believedMastery();
        int firstLevel = s.nextLevel();
        for (int i = 0; i < 6; i++) s.observe(s.nextLevel(), true);
        assertThat(s.believedMastery()).isGreaterThan(before);
        assertThat(s.nextLevel()).isGreaterThanOrEqualTo(firstLevel);
    }

    // ── Simulator ─────────────────────────────────────────────────

    @Test
    void runsAreDeterministicAndPoliciesSeeTheSameStudent() {
        Simulator sim = new Simulator(model);
        Simulator.Student student = Simulator.population(1, -1.0, 99).get(0);
        Simulator.Run a = sim.run(student, DifficultyPolicy.fixed(), 20);
        Simulator.Run b = sim.run(student, DifficultyPolicy.fixed(), 20);
        assertThat(a.steps()).isEqualTo(b.steps());

        Simulator.Run adaptive = sim.run(student, DifficultyPolicy.adaptiveEngine(), 20);
        // Same first item (level 1) and same answer draw → the student side of step 1 is identical
        // (only the policy's own belief differs).
        Simulator.Step f = a.steps().get(0), g = adaptive.steps().get(0);
        assertThat(g.level()).isEqualTo(f.level());
        assertThat(g.pCorrect()).isEqualTo(f.pCorrect());
        assertThat(g.correct()).isEqualTo(f.correct());
        assertThat(g.thetaAfter()).isEqualTo(f.thetaAfter());
    }

    @Test
    void abilityNeverDecreasesAndQuittersStopLearning() {
        Simulator sim = new Simulator(model.withQuitRate(0.5));
        for (Simulator.Student st : Simulator.population(200, -2.0, 5)) {
            Simulator.Run run = sim.run(st, DifficultyPolicy.fixed(), 30);
            double prev = st.theta0();
            boolean quit = false;
            for (Simulator.Step s : run.steps()) {
                assertThat(s.thetaAfter()).isGreaterThanOrEqualTo(prev);
                if (quit) {
                    assertThat(s.active()).isFalse();
                    assertThat(s.thetaAfter()).isEqualTo(prev);
                }
                if (!s.active()) quit = true;
                prev = s.thetaAfter();
            }
        }
    }

    @Test
    void headlineOrderingHoldsOnASmallPopulation() {
        // Regression guard for the documented results: random is clearly worst; Elo's mastery claims are
        // far more accurate than the current engine's.
        Simulator sim = new Simulator(model);
        List<Simulator.Student> pop = Simulator.population(600, -1.0, 2024);
        PolicySummary fixed = summarise(sim, pop, DifficultyPolicy.fixed());
        PolicySummary random = summarise(sim, pop, DifficultyPolicy.random());
        PolicySummary engine = summarise(sim, pop, DifficultyPolicy.adaptiveEngine());
        PolicySummary elo = summarise(sim, pop, DifficultyPolicy.eloTarget(0.5));

        assertThat(random.masteredByEnd()).isLessThan(engine.masteredByEnd()).isLessThan(fixed.masteredByEnd());
        assertThat(engine.engineClaimPrecision()).isLessThan(0.10);
        assertThat(elo.engineClaimPrecision()).isGreaterThan(0.80);
        assertThat(fixed.engineClaimPrecision()).isNaN(); // fixed ramp has no belief
    }

    private static PolicySummary summarise(Simulator sim, List<Simulator.Student> pop, DifficultyPolicy p) {
        List<Simulator.Run> runs = new ArrayList<>();
        for (Simulator.Student s : pop) runs.add(sim.run(s, p, 30));
        return PolicySummary.of("test", p.name(), runs);
    }

    // ── BKT fitter ────────────────────────────────────────────────

    @Test
    void fitterRecoversKnownParameters() {
        double[] truth = {0.20, 0.15, 0.20, 0.08};
        List<List<Boolean>> seqs = EvaluationRunner.sampleBkt(new Random(11), truth, 600, 12);

        BktFitter.Fit fit = BktFitter.fit(seqs);

        assertThat(fit.pInit()).isCloseTo(truth[0], within(0.06));
        assertThat(fit.pTransit()).isCloseTo(truth[1], within(0.05));
        assertThat(fit.pGuess()).isCloseTo(truth[2], within(0.05));
        assertThat(fit.pSlip()).isCloseTo(truth[3], within(0.04));
        double llTrue = BktFitter.logLikelihood(seqs, truth[0], truth[1], truth[2], truth[3]);
        assertThat(fit.logLikelihood()).isGreaterThanOrEqualTo(llTrue - 1e-6); // MLE at least as good as truth
        assertThat(fit.observations()).isEqualTo(600 * 12);
    }

    @Test
    void fitterRespectsIdentifiabilityBounds() {
        // All-correct data would push guess→1 without the bound.
        List<List<Boolean>> allCorrect = new ArrayList<>();
        for (int i = 0; i < 50; i++) allCorrect.add(List.of(true, true, true, true));
        BktFitter.Fit fit = BktFitter.fit(allCorrect);
        assertThat(fit.pGuess()).isLessThanOrEqualTo(0.40);
        assertThat(fit.pSlip()).isLessThanOrEqualTo(0.30);
    }
}
