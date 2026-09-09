package com.neurolearn.repository;

import com.neurolearn.entity.TopicProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicProgressRepository extends JpaRepository<TopicProgress, Long> {
    Optional<TopicProgress> findByStudentProfileIdAndTopic(Long studentProfileId, String topic);
    List<TopicProgress> findByStudentProfileId(Long studentProfileId);
}
