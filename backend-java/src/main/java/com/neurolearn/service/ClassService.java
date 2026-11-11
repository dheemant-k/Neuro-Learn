package com.neurolearn.service;

import com.neurolearn.dto.StudentClassResponse;
import com.neurolearn.dto.TeacherClassResponse;
import com.neurolearn.entity.ClassEnrollment;
import com.neurolearn.entity.Role;
import com.neurolearn.entity.SchoolClass;
import com.neurolearn.entity.StudentProfile;
import com.neurolearn.entity.User;
import com.neurolearn.exception.ForbiddenException;
import com.neurolearn.exception.NotFoundException;
import com.neurolearn.repository.ClassEnrollmentRepository;
import com.neurolearn.repository.SchoolClassRepository;
import com.neurolearn.repository.StudentProfileRepository;
import com.neurolearn.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Classes and enrollments. Owns the rule "a teacher only sees students enrolled in
 * one of their own classes", which TeacherService relies on.
 */
@Service
public class ClassService {

    /** No 0/O or 1/I/L, so codes read cleanly off a whiteboard. */
    static final String JOIN_CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    static final int JOIN_CODE_LENGTH = 6;
    private static final int MAX_CODE_ATTEMPTS = 20;

    private final SchoolClassRepository classRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository profileRepository;
    private final SecureRandom random = new SecureRandom();

    public ClassService(
            SchoolClassRepository classRepository,
            ClassEnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            StudentProfileRepository profileRepository
    ) {
        this.classRepository = classRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    // ── Teacher side ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TeacherClassResponse> listTeacherClasses(String teacherEmail) {
        User teacher = requireUser(teacherEmail, Role.TEACHER);
        return classRepository.findByTeacherIdOrderByCreatedAtAsc(teacher.getId()).stream()
                .map(this::toTeacherResponse)
                .toList();
    }

    @Transactional
    public TeacherClassResponse createClass(String teacherEmail, String name) {
        User teacher = requireUser(teacherEmail, Role.TEACHER);
        SchoolClass created = classRepository.save(new SchoolClass(teacher, name.trim(), newJoinCode()));
        return toTeacherResponse(created);
    }

    @Transactional
    public void removeStudent(String teacherEmail, Long classId, Long studentProfileId) {
        SchoolClass schoolClass = getOwnedClass(teacherEmail, classId);
        ClassEnrollment enrollment = enrollmentRepository
                .findBySchoolClassIdAndStudentProfileId(schoolClass.getId(), studentProfileId)
                .orElseThrow(() -> new NotFoundException("Student " + studentProfileId + " is not in this class"));
        enrollmentRepository.delete(enrollment);
    }

    /**
     * Student profile ids a teacher may see: one class (must be theirs) or, with
     * {@code classId == null}, every class they teach.
     */
    @Transactional(readOnly = true)
    public Set<Long> visibleStudentProfileIds(String teacherEmail, Long classId) {
        List<Long> classIds = classId != null
                ? List.of(getOwnedClass(teacherEmail, classId).getId())
                : classRepository.findByTeacherIdOrderByCreatedAtAsc(requireUser(teacherEmail, Role.TEACHER).getId())
                        .stream().map(SchoolClass::getId).toList();
        if (classIds.isEmpty()) {
            return Set.of();
        }
        return enrollmentRepository.findBySchoolClassIdIn(classIds).stream()
                .map(e -> e.getStudentProfile().getId())
                .collect(Collectors.toSet());
    }

    /** 404 if the student doesn't exist, 403 if they aren't in any of this teacher's classes. */
    @Transactional(readOnly = true)
    public void requireTeacherCanSeeStudent(String teacherEmail, Long studentProfileId) {
        User teacher = requireUser(teacherEmail, Role.TEACHER);
        if (!profileRepository.existsById(studentProfileId)) {
            throw new NotFoundException("Student not found: " + studentProfileId);
        }
        if (!enrollmentRepository.existsBySchoolClassTeacherIdAndStudentProfileId(teacher.getId(), studentProfileId)) {
            throw new ForbiddenException("This student is not in any of your classes");
        }
    }

    // ── Student side ──────────────────────────────────────────────

    /** Joins by code (case-insensitive). Joining a class you're already in just returns it. */
    @Transactional
    public StudentClassResponse joinClass(String studentEmail, String joinCode) {
        StudentProfile profile = requireStudentProfile(studentEmail);
        SchoolClass schoolClass = classRepository.findByJoinCode(normalizeCode(joinCode))
                .orElseThrow(() -> new NotFoundException("No class found with that code"));

        ClassEnrollment enrollment = enrollmentRepository
                .findBySchoolClassIdAndStudentProfileId(schoolClass.getId(), profile.getId())
                .orElseGet(() -> enrollmentRepository.save(new ClassEnrollment(schoolClass, profile)));
        return toStudentResponse(enrollment);
    }

    @Transactional(readOnly = true)
    public List<StudentClassResponse> listStudentClasses(String studentEmail) {
        StudentProfile profile = requireStudentProfile(studentEmail);
        return enrollmentRepository.findByStudentProfileId(profile.getId()).stream()
                .map(this::toStudentResponse)
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────

    private SchoolClass getOwnedClass(String teacherEmail, Long classId) {
        User teacher = requireUser(teacherEmail, Role.TEACHER);
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new NotFoundException("Class not found: " + classId));
        if (!schoolClass.getTeacher().getId().equals(teacher.getId())) {
            throw new ForbiddenException("This class belongs to another teacher");
        }
        return schoolClass;
    }

    private User requireUser(String email, Role role) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() != role) {
            // Defence in depth: SecurityConfig already restricts these paths by role.
            throw new ForbiddenException("Only " + role + " accounts can do this");
        }
        return user;
    }

    private StudentProfile requireStudentProfile(String email) {
        User student = requireUser(email, Role.STUDENT);
        return profileRepository.findByUserId(student.getId())
                .orElseThrow(() -> new NotFoundException("Student profile not found"));
    }

    static String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private String newJoinCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            StringBuilder sb = new StringBuilder(JOIN_CODE_LENGTH);
            for (int i = 0; i < JOIN_CODE_LENGTH; i++) {
                sb.append(JOIN_CODE_ALPHABET.charAt(random.nextInt(JOIN_CODE_ALPHABET.length())));
            }
            String code = sb.toString();
            if (!classRepository.existsByJoinCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique join code");
    }

    private TeacherClassResponse toTeacherResponse(SchoolClass c) {
        return new TeacherClassResponse(c.getId(), c.getName(), c.getJoinCode(),
                enrollmentRepository.countBySchoolClassId(c.getId()), c.getCreatedAt());
    }

    private StudentClassResponse toStudentResponse(ClassEnrollment e) {
        SchoolClass c = e.getSchoolClass();
        return new StudentClassResponse(c.getId(), c.getName(), c.getTeacher().getName(), e.getJoinedAt());
    }
}
