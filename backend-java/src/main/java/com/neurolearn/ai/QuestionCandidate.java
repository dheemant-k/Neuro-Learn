package com.neurolearn.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * One multiple-choice question as proposed by the LLM (or edited by a teacher),
 * before it is saved. {@code mistakes} maps each WRONG option (A–D) to the student
 * mistake it represents.
 */
public record QuestionCandidate(
        String question,
        String a,
        String b,
        String c,
        String d,
        String correct,
        String explanation,
        Map<String, String> mistakes
) {
    static final int MAX_QUESTION = 1000;
    static final int MAX_OPTION = 300;
    static final int MAX_EXPLANATION = 2000;
    static final int MAX_MISTAKE = 300;
    static final List<String> KEYS = List.of("A", "B", "C", "D");

    public String option(String key) {
        return switch (key) {
            case "A" -> a;
            case "B" -> b;
            case "C" -> c;
            case "D" -> d;
            default -> null;
        };
    }

    /** Upper-cased, trimmed correct key, or null if it isn't A–D. */
    public String correctKey() {
        if (correct == null) return null;
        String k = correct.trim().toUpperCase(Locale.ROOT);
        return KEYS.contains(k) ? k : null;
    }

    /**
     * Structural checks (no maths): returns the first problem found, or empty if the
     * candidate is well-formed. Maths correctness is checked separately (self-check + teacher).
     */
    public Optional<String> problem() {
        if (blank(question)) return Optional.of("Question text is empty");
        if (question.length() > MAX_QUESTION) return Optional.of("Question text is too long");
        for (String k : KEYS) {
            String o = option(k);
            if (blank(o)) return Optional.of("Option " + k + " is empty");
            if (o.length() > MAX_OPTION) return Optional.of("Option " + k + " is too long");
        }
        String key = correctKey();
        if (key == null) return Optional.of("Correct answer must be A, B, C or D");
        Map<String, String> seen = new LinkedHashMap<>();
        for (String k : KEYS) {
            String norm = normalize(option(k));
            if (seen.containsKey(norm)) {
                return Optional.of("Options " + seen.get(norm) + " and " + k + " are the same");
            }
            seen.put(norm, k);
        }
        if (blank(explanation)) return Optional.of("Explanation is empty");
        if (explanation.length() > MAX_EXPLANATION) return Optional.of("Explanation is too long");
        for (String k : KEYS) {
            if (k.equals(key)) continue;
            String m = mistakes == null ? null : mistakes.get(k);
            if (blank(m)) return Optional.of("Wrong option " + k + " has no misconception");
            if (m.length() > MAX_MISTAKE) return Optional.of("Misconception for " + k + " is too long");
        }
        return Optional.empty();
    }

    /** Lower-case, single spaces, no spaces around = + - so "x=5" and "x = 5" compare equal. */
    public static String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .replaceAll("\\s*([=+\\-*/()])\\s*", "$1")
                .trim();
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
