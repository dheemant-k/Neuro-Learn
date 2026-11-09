package com.neurolearn.adaptive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BktEngineTest {

    private AdaptiveEngineService engine;

    @BeforeEach
    void setUp() {
        engine = new AdaptiveEngineService(BktParameters.defaultParameters());
    }

    @Test
    void testInitialCorrectAnswerIncreasesKnowledgeBelief() {
        double prior = 0.20;
        BktUpdateResult result = engine.updateKnowledge(prior, true);

        assertTrue(result.updatedPKnow() > prior, "Updated P(L) must increase after correct answer");
        assertTrue(result.posteriorPKnow() > prior, "Posterior must be higher than prior");
        assertFalse(result.isMastered(), "Single question should not immediately master the topic");
        assertTrue(result.explanation().contains("Bayesian Evidence: Correct answer"));
    }

    @Test
    void testIncorrectAnswerReducesPosterior() {
        double prior = 0.60;
        BktUpdateResult result = engine.updateKnowledge(prior, false);

        assertTrue(result.posteriorPKnow() < prior, "Posterior must drop when answering incorrectly");
        assertTrue(result.updatedPKnow() < prior, "Updated knowledge should decrease after slip/error");
    }

    @Test
    void testRepeatedCorrectAnswersReachMastery() {
        double currentP = 0.20;
        int steps = 0;
        while (currentP < 0.85 && steps < 10) {
            BktUpdateResult res = engine.updateKnowledge(currentP, true);
            currentP = res.updatedPKnow();
            steps++;
        }

        assertTrue(currentP >= 0.85, "Repeated correct answers must reach mastery threshold (0.85)");
        BktUpdateResult finalRes = engine.updateKnowledge(currentP, true);
        assertTrue(finalRes.isMastered(), "isMastered must be true once threshold is crossed");
    }

    @Test
    void testInitialDiagnosticStateDoesNotHaveBogusStreak() {
        AdaptiveDecision decision = engine.evaluateBkt(0.20, 1, 0);

        assertEquals(1, decision.nextDifficulty(), "First question must be Level 1");
        assertEquals(DifficultyAction.MAINTAIN, decision.action());
        assertFalse(decision.reason().contains("0 correct"), "Must not display 'streak (0 correct)'");
        assertTrue(decision.reason().contains("Baseline knowledge state P(L)=20%"));
    }

    @Test
    void testDifficultyTiersFollowBayesianBelief() {
        // Tier 1: P(L) < 0.40 -> Level 1
        AdaptiveDecision d1 = engine.evaluateBkt(0.25, 2, 2);
        assertEquals(1, d1.nextDifficulty());
        assertEquals(DifficultyAction.DECREASE, d1.action());

        // Tier 2: 0.40 <= P(L) < 0.75 -> Level 2
        AdaptiveDecision d2 = engine.evaluateBkt(0.55, 1, 2);
        assertEquals(2, d2.nextDifficulty());
        assertEquals(DifficultyAction.INCREASE, d2.action());

        // Tier 3: P(L) >= 0.75 -> Level 3
        AdaptiveDecision d3 = engine.evaluateBkt(0.88, 2, 4);
        assertEquals(3, d3.nextDifficulty());
        assertEquals(DifficultyAction.INCREASE, d3.action());
        assertTrue(d3.isMastered());
    }

    @Test
    void testHigherGuessProbabilityMakesCorrectAnswerWeakerEvidence() {
        double prior = 0.30;
        BktUpdateResult normal = engine.updateKnowledge(prior, true);          // P(G)=0.25 (4 options)
        BktUpdateResult afterFiftyFifty = engine.updateKnowledge(prior, true, 0.5); // 2 options left

        assertTrue(afterFiftyFifty.posteriorPKnow() < normal.posteriorPKnow(),
                "A correct answer with 2 options left should raise P(L) less");
        assertTrue(afterFiftyFifty.posteriorPKnow() > prior, "It is still some evidence of knowing");
    }

    @Test
    void testDefaultOverloadMatchesExplicitDefaultGuess() {
        BktUpdateResult a = engine.updateKnowledge(0.42, false);
        BktUpdateResult b = engine.updateKnowledge(0.42, false, engine.getParameters().pGuess());
        assertEquals(a.updatedPKnow(), b.updatedPKnow(), 1e-12);
    }
}
