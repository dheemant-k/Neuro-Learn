package com.neurolearn.repository;

import com.neurolearn.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByTopicAndDifficulty(String topic, Integer difficulty);
    List<Question> findByTopic(String topic);

    @Query(value = "SELECT * FROM questions WHERE topic = :topic AND difficulty = :difficulty AND id NOT IN (:excludeIds) ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Question> findRandomQuestionExcluding(@Param("topic") String topic, @Param("difficulty") Integer difficulty, @Param("excludeIds") List<Long> excludeIds);
}
