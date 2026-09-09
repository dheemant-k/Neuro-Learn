package com.neurolearn.repository;

import com.neurolearn.entity.LearningSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningSessionRepository extends JpaRepository<LearningSession, Long> {
    List<LearningSession> findByStudentProfileIdOrderByStartTimeDesc(Long studentProfileId);
}
