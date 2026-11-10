package com.neurolearn.ai;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionCandidateTest {

    private static Map<String, String> mistakes() {
        return new HashMap<>(Map.of("A", "", "B", "added instead of subtracting", "C", "forgot to divide", "D", "sign error"));
    }

    private static QuestionCandidate valid() {
        return new QuestionCandidate("Solve 2x + 3 = 11", "x = 4", "x = 7", "x = 8", "x = -4", "A",
                "Subtract 3, divide by 2.", mistakes());
    }

    @Test
    void wellFormedCandidateHasNoProblem() {
        assertThat(valid().problem()).isEmpty();
        assertThat(valid().correctKey()).isEqualTo("A");
    }

    @Test
    void correctKeyIsNormalisedAndValidated() {
        QuestionCandidate lower = new QuestionCandidate("q", "1", "2", "3", "4", " b ", "e", mistakes());
        assertThat(lower.correctKey()).isEqualTo("B");
        QuestionCandidate bad = new QuestionCandidate("q", "1", "2", "3", "4", "E", "e", mistakes());
        assertThat(bad.problem()).contains("Correct answer must be A, B, C or D");
    }

    @Test
    void duplicateOptionsAreRejectedEvenWithDifferentSpacing() {
        QuestionCandidate dup = new QuestionCandidate("q", "x = 4", "x=4", "x = 8", "x = -4", "A", "e", mistakes());
        assertThat(dup.problem()).contains("Options A and B are the same");
    }

    @Test
    void emptyFieldsAreRejected() {
        assertThat(new QuestionCandidate(" ", "1", "2", "3", "4", "A", "e", mistakes()).problem()).contains("Question text is empty");
        assertThat(new QuestionCandidate("q", "1", "", "3", "4", "A", "e", mistakes()).problem()).contains("Option B is empty");
        assertThat(new QuestionCandidate("q", "1", "2", "3", "4", "A", null, mistakes()).problem()).contains("Explanation is empty");
    }

    @Test
    void everyWrongOptionNeedsAMisconceptionButTheCorrectOneDoesNot() {
        Map<String, String> m = mistakes();
        m.put("C", " ");
        QuestionCandidate missing = new QuestionCandidate("q", "1", "2", "3", "4", "A", "e", m);
        assertThat(missing.problem()).contains("Wrong option C has no misconception");

        Map<String, String> correctOnlyEmpty = mistakes();
        correctOnlyEmpty.put("D", "");
        correctOnlyEmpty.put("A", "something");
        QuestionCandidate dCorrect = new QuestionCandidate("q", "1", "2", "3", "4", "D", "e", correctOnlyEmpty);
        assertThat(dCorrect.problem()).isEmpty();
    }

    @Test
    void overlongTextIsRejected() {
        QuestionCandidate longQ = new QuestionCandidate("x".repeat(1001), "1", "2", "3", "4", "A", "e", mistakes());
        assertThat(longQ.problem()).contains("Question text is too long");
    }

    @Test
    void normalizeIgnoresCaseAndSpacingAroundOperators() {
        assertThat(QuestionCandidate.normalize("  X =  4 ")).isEqualTo(QuestionCandidate.normalize("x=4"));
        assertThat(QuestionCandidate.normalize("Solve 2x + 3 = 11")).isEqualTo(QuestionCandidate.normalize("solve 2x+3=11"));
        assertThat(QuestionCandidate.normalize("x = 4")).isNotEqualTo(QuestionCandidate.normalize("x = 5"));
    }
}
