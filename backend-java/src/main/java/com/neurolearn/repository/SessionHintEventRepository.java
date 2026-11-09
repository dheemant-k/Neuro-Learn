package com.neurolearn.repository;

import com.neurolearn.entity.SessionHintEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionHintEventRepository extends JpaRepository<SessionHintEvent, Long> {
    List<SessionHintEvent> findBySessionIdAndQuestionId(Long sessionId, Long questionId);
    Optional<SessionHintEvent> findBySessionIdAndQuestionIdAndHintType(Long sessionId, Long questionId, SessionHintEvent.HintType hintType);
}
