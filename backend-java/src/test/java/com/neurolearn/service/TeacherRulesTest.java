package com.neurolearn.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Roster status and curriculum-bottleneck rules behind the Teacher dashboard. */
class TeacherRulesTest {

    @ParameterizedTest(name = "pKnow={0}, hard correct={1} → {2}")
    @CsvSource({
            "0.20, 5, Needs Attention",
            "0.59, 0, Needs Attention",
            "0.60, 0, On Track",
            "0.84, 9, On Track",
            "0.85, 0, On Track",   // high P(L) from easy answers alone is not mastery
            "1.00, 1, On Track",
            "0.85, 2, Mastered",
            "1.00, 3, Mastered"
    })
    void studentStatus(double pKnow, long hardCorrect, String expected) {
        assertThat(TeacherService.studentStatus(pKnow, hardCorrect)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "avg={0}, struggling={1}, mastered={2} → alert={3}")
    @CsvSource({
            "0.30, 0, 0, false",  // nobody struggling → never an alert
            "0.50, 1, 5, true",   // low class average
            "0.80, 3, 3, true",   // strugglers ≥ masters
            "0.80, 1, 4, false",  // healthy average, mostly masters
            "0.65, 1, 2, false"   // exactly at the average threshold, masters outnumber
    })
    void bottleneck(double avg, int struggling, int mastered, boolean expected) {
        assertThat(TeacherService.isBottleneck(avg, struggling, mastered)).isEqualTo(expected);
    }
}
