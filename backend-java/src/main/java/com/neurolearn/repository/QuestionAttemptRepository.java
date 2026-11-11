package com.neurolearn.repository;

import com.neurolearn.entity.QuestionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface QuestionAttemptRepository extends JpaRepository<QuestionAttempt, Long> {
    List<QuestionAttempt> findByStudentProfileIdOrderByTimestampDesc(Long studentProfileId);
    List<QuestionAttempt> findBySessionId(Long sessionId);

    /**
     * Rows of [studentProfileId, topic, count] — correct answers on questions of the given difficulty.
     * Uses the question's difficulty, so attempts from before V9 (no difficulty_served) count too.
     */
    @Query("select a.studentProfile.id, a.question.topic, count(a) from QuestionAttempt a "
            + "where a.correct = true and a.question.difficulty = :difficulty and a.studentProfile.id in :studentIds "
            + "group by a.studentProfile.id, a.question.topic")
    List<Object[]> countCorrectByStudentAndTopicAtDifficulty(@Param("studentIds") Collection<Long> studentIds,
                                                             @Param("difficulty") int difficulty);
}
