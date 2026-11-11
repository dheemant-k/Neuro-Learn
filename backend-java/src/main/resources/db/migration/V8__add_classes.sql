-- V8__add_classes.sql: teacher-owned classes that students join with a code.
-- Teachers only see analytics for students enrolled in their own classes.

CREATE TABLE classes (
    id              BIGSERIAL PRIMARY KEY,
    teacher_user_id BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    join_code       VARCHAR(12)  NOT NULL UNIQUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_classes_teacher ON classes(teacher_user_id);

CREATE TABLE class_enrollments (
    id                 BIGSERIAL PRIMARY KEY,
    class_id           BIGINT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    student_profile_id BIGINT NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    joined_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_class_student UNIQUE (class_id, student_profile_id)
);

CREATE INDEX idx_enrollments_student ON class_enrollments(student_profile_id);

-- Demo class for the V5 demo teacher, with the three seeded demo students.
-- (testhero@ is deliberately not enrolled.)
INSERT INTO classes (teacher_user_id, name, join_code)
SELECT u.id, 'Gotham Academy · Algebra 1', 'GOTHAM'
FROM users u WHERE u.email = 'teacher@neurolearn.com'
ON CONFLICT (join_code) DO NOTHING;

INSERT INTO class_enrollments (class_id, student_profile_id)
SELECT c.id, sp.id
FROM classes c
JOIN student_profiles sp ON TRUE
JOIN users u ON u.id = sp.user_id
WHERE c.join_code = 'GOTHAM'
  AND u.email IN ('bruce@neurolearn.com', 'dick@neurolearn.com', 'jason@neurolearn.com')
ON CONFLICT (class_id, student_profile_id) DO NOTHING;
