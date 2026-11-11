package com.neurolearn.service;

import com.neurolearn.repository.QuestionAttemptRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The one definition of "Mastered" used everywhere (roadmap badge, dashboards, teacher roster and
 * bottlenecks, parent report cards, quiz responses).
 *
 * <p>BKT alone is not enough: it ignores item difficulty, so a few correct easy answers push P(L)
 * past 0.85. In the Phase 5 simulation that badge was almost never deserved (docs/evaluation.md §4.2).
 * So mastery must also be <b>earned on hard questions</b>: P(L) ≥ 0.85 <i>and</i> at least
 * {@value #MIN_HARD_CORRECT} correct answers on level-3 questions in that topic.
 */
@Component
public class MasteryRule {

    public static final double P_KNOW_THRESHOLD = 0.85;
    public static final int MIN_HARD_CORRECT = 2;
    public static final int HARD_LEVEL = 3;

    private final QuestionAttemptRepository attemptRepository;

    public MasteryRule(QuestionAttemptRepository attemptRepository) {
        this.attemptRepository = attemptRepository;
    }

    public static boolean isMastered(double pKnow, long hardCorrect) {
        return pKnow >= P_KNOW_THRESHOLD && hardCorrect >= MIN_HARD_CORRECT;
    }

    /** How many more correct hard answers are needed (0 once the hard-question part is met). */
    public static long hardCorrectStillNeeded(long hardCorrect) {
        return Math.max(0, MIN_HARD_CORRECT - hardCorrect);
    }

    /** topic → correct answers on level-3 questions, for one student. */
    public Map<String, Long> hardCorrectByTopic(Long studentProfileId) {
        return hardCorrectByStudentAndTopic(List.of(studentProfileId)).getOrDefault(studentProfileId, Map.of());
    }

    public long hardCorrect(Long studentProfileId, String topic) {
        return hardCorrectByTopic(studentProfileId).getOrDefault(topic, 0L);
    }

    /** student → topic → correct level-3 answers, for many students in one query. */
    public Map<Long, Map<String, Long>> hardCorrectByStudentAndTopic(Collection<Long> studentProfileIds) {
        Map<Long, Map<String, Long>> out = new HashMap<>();
        if (studentProfileIds.isEmpty()) return out;
        for (Object[] row : attemptRepository.countCorrectByStudentAndTopicAtDifficulty(studentProfileIds, HARD_LEVEL)) {
            Long student = (Long) row[0];
            String topic = (String) row[1];
            Long count = ((Number) row[2]).longValue();
            out.computeIfAbsent(student, k -> new HashMap<>()).put(topic, count);
        }
        return out;
    }
}
