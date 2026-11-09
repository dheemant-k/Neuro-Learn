package com.neurolearn.repository;

import com.neurolearn.entity.ClassEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {
    List<ClassEnrollment> findBySchoolClassIdIn(Collection<Long> classIds);
    List<ClassEnrollment> findByStudentProfileId(Long studentProfileId);
    Optional<ClassEnrollment> findBySchoolClassIdAndStudentProfileId(Long classId, Long studentProfileId);
    long countBySchoolClassId(Long classId);
    /** Is this student in any class taught by this teacher? */
    boolean existsBySchoolClassTeacherIdAndStudentProfileId(Long teacherUserId, Long studentProfileId);
}
