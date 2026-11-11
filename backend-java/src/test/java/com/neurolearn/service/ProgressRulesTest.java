package com.neurolearn.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Roadmap unlock and mastery rules in ProgressService (documented in README §26). */
class ProgressRulesTest {

    @ParameterizedTest(name = "pKnow={0}, attempts={1}, correct={2} → completed={3}")
    @CsvSource({
            "0.20, 0, 0, false",   // untouched section
            "0.39, 5, 2, false",   // just under both thresholds
            "0.40, 1, 0, true",    // P(L) threshold reached
            "0.20, 3, 3, true",    // 3 correct reached even with low P(L)
            "0.20, 0, 3, false",   // correct count without attempts is not trusted
            "0.95, 10, 10, true"
    })
    void sectionCompletion(double pKnow, int attempts, int correct, boolean expected) {
        assertThat(ProgressService.isSectionCompleted(pKnow, attempts, correct)).isEqualTo(expected);
    }

    @Test
    void masteryIsStricterThanUnlock() {
        assertThat(ProgressService.isMastered(0.84)).isFalse();
        assertThat(ProgressService.isMastered(0.85)).isTrue();
        assertThat(ProgressService.isSectionCompleted(0.50, 1, 0)).isTrue();
        assertThat(ProgressService.isMastered(0.50)).isFalse();
    }

    @ParameterizedTest(name = "{0}/{1} sections done → boss unlocked={2}")
    @CsvSource({
            "0, 6, false",
            "2, 6, false",
            "3, 6, true",
            "6, 6, true",
            "2, 2, true"    // short chapter: all sections done
    })
    void bossUnlock(int completed, int total, boolean expected) {
        assertThat(ProgressService.isBossUnlocked(completed, total)).isEqualTo(expected);
    }
}
