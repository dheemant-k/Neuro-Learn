package com.neurolearn.repository;

import com.neurolearn.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {
    List<SchoolClass> findByTeacherIdOrderByCreatedAtAsc(Long teacherUserId);
    Optional<SchoolClass> findByJoinCode(String joinCode);
    boolean existsByJoinCode(String joinCode);
}
