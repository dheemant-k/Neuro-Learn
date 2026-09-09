package com.neurolearn.repository;

import com.neurolearn.entity.QuestionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionAttemptRepository extends JpaRepository<QuestionAttempt, Long> {
    List<QuestionAttempt> findByStudentProfileIdOrderByTimestampDesc(Long studentProfileId);
    List<QuestionAttempt> findBySessionId(Long sessionId);
}
