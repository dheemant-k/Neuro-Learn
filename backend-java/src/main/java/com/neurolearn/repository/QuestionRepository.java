package com.neurolearn.repository;

import com.neurolearn.entity.Question;
import com.neurolearn.entity.QuestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * The "serving" queries below return APPROVED questions only, so pending/rejected
 * generated questions can never reach a student. Review queries are at the bottom.
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    // ── Serving (approved only) ───────────────────────────────────

    @Query("select q from Question q where q.status = com.neurolearn.entity.QuestionStatus.APPROVED")
    List<Question> findAllApproved();

    @Query("select q from Question q where q.topic = :topic and q.difficulty = :difficulty and q.status = com.neurolearn.entity.QuestionStatus.APPROVED")
    List<Question> findByTopicAndDifficulty(@Param("topic") String topic, @Param("difficulty") Integer difficulty);

    @Query("select q from Question q where q.topic = :topic and q.status = com.neurolearn.entity.QuestionStatus.APPROVED")
    List<Question> findByTopic(@Param("topic") String topic);

    @Query("select q from Question q where q.topic = :topic and q.difficulty = :difficulty and q.characterTheme = :characterTheme and q.status = com.neurolearn.entity.QuestionStatus.APPROVED")
    List<Question> findByTopicAndDifficultyAndCharacterTheme(@Param("topic") String topic, @Param("difficulty") Integer difficulty, @Param("characterTheme") String characterTheme);

    @Query("select q from Question q where q.topic = :topic and q.characterTheme = :characterTheme and q.status = com.neurolearn.entity.QuestionStatus.APPROVED")
    List<Question> findByTopicAndCharacterTheme(@Param("topic") String topic, @Param("characterTheme") String characterTheme);

    @Query(value = "SELECT * FROM questions WHERE topic = :topic AND difficulty = :difficulty AND status = 'APPROVED' ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Question> findRandomQuestionByTopicAndDifficulty(@Param("topic") String topic, @Param("difficulty") Integer difficulty);

    @Query(value = "SELECT * FROM questions WHERE topic = :topic AND status = 'APPROVED' ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Question> findRandomQuestionByTopic(@Param("topic") String topic);

    @Query(value = "SELECT * FROM questions WHERE topic = :topic AND difficulty = :difficulty AND character_theme = :characterTheme AND status = 'APPROVED' ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Question> findRandomQuestionByTopicAndDifficultyAndCharacterTheme(@Param("topic") String topic, @Param("difficulty") Integer difficulty, @Param("characterTheme") String characterTheme);

    @Query(value = "SELECT * FROM questions WHERE topic = :topic AND character_theme = :characterTheme AND status = 'APPROVED' ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Question> findRandomQuestionByTopicAndCharacterTheme(@Param("topic") String topic, @Param("characterTheme") String characterTheme);

    @Query(value = "SELECT DISTINCT topic FROM questions WHERE status = 'APPROVED' ORDER BY topic", nativeQuery = true)
    List<String> findDistinctTopics();

    // ── Generation & review ───────────────────────────────────────

    /** All question texts in a topic, any status — for duplicate detection. */
    @Query("select q.questionText from Question q where q.topic = :topic")
    List<String> findAllTextsByTopic(@Param("topic") String topic);

    List<Question> findByCreatedByIdAndStatusOrderByCreatedAtDesc(Long createdByUserId, QuestionStatus status);

    long countByCreatedByIdAndCreatedAtAfter(Long createdByUserId, OffsetDateTime since);
}
