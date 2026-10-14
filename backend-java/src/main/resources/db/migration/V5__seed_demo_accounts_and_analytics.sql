-- V5__seed_demo_accounts_and_analytics.sql
-- Seed script for Demo Accounts (Teacher, Parent, Students) with rich Bayesian analytics
-- Password for all demo accounts: password123 (BCrypt: $2a$10$CmuRk6TAukSnC9LT04GWYORO.WGXdMtdY/NT6es2yu8KxNPehMRay)

-- 1. Insert Demo Users
INSERT INTO users (name, email, password_hash, role, created_at)
VALUES 
    ('Professor Wayne', 'teacher@neurolearn.com', '$2a$10$CmuRk6TAukSnC9LT04GWYORO.WGXdMtdY/NT6es2yu8KxNPehMRay', 'TEACHER', CURRENT_TIMESTAMP),
    ('Martha Wayne', 'parent@neurolearn.com', '$2a$10$CmuRk6TAukSnC9LT04GWYORO.WGXdMtdY/NT6es2yu8KxNPehMRay', 'PARENT', CURRENT_TIMESTAMP),
    ('Bruce Wayne', 'bruce@neurolearn.com', '$2a$10$CmuRk6TAukSnC9LT04GWYORO.WGXdMtdY/NT6es2yu8KxNPehMRay', 'STUDENT', CURRENT_TIMESTAMP),
    ('Dick Grayson', 'dick@neurolearn.com', '$2a$10$CmuRk6TAukSnC9LT04GWYORO.WGXdMtdY/NT6es2yu8KxNPehMRay', 'STUDENT', CURRENT_TIMESTAMP),
    ('Jason Todd', 'jason@neurolearn.com', '$2a$10$CmuRk6TAukSnC9LT04GWYORO.WGXdMtdY/NT6es2yu8KxNPehMRay', 'STUDENT', CURRENT_TIMESTAMP),
    ('Test Hero', 'testhero@neurolearn.com', '$2a$10$CmuRk6TAukSnC9LT04GWYORO.WGXdMtdY/NT6es2yu8KxNPehMRay', 'STUDENT', CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- 2. Insert Student Profiles
INSERT INTO student_profiles (user_id, current_level, overall_accuracy, total_xp, p_know)
SELECT u.id, 5, 88.5, 750, 0.88
FROM users u WHERE u.email = 'bruce@neurolearn.com'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO student_profiles (user_id, current_level, overall_accuracy, total_xp, p_know)
SELECT u.id, 3, 72.0, 410, 0.72
FROM users u WHERE u.email = 'dick@neurolearn.com'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO student_profiles (user_id, current_level, overall_accuracy, total_xp, p_know)
SELECT u.id, 1, 42.0, 140, 0.35
FROM users u WHERE u.email = 'jason@neurolearn.com'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO student_profiles (user_id, current_level, overall_accuracy, total_xp, p_know)
SELECT u.id, 1, 0.0, 0, 0.20
FROM users u WHERE u.email = 'testhero@neurolearn.com'
ON CONFLICT (user_id) DO NOTHING;

-- 3. Insert Topic Progress for Bruce Wayne (High Performer)
INSERT INTO topic_progress (student_id, topic, mastery_score, attempts, correct_count, p_know)
SELECT sp.id, 'intro', 90.0, 10, 9, 0.92
FROM student_profiles sp JOIN users u ON sp.user_id = u.id WHERE u.email = 'bruce@neurolearn.com'
ON CONFLICT (student_id, topic) DO NOTHING;

INSERT INTO topic_progress (student_id, topic, mastery_score, attempts, correct_count, p_know)
SELECT sp.id, 'graphing', 87.5, 8, 7, 0.89
FROM student_profiles sp JOIN users u ON sp.user_id = u.id WHERE u.email = 'bruce@neurolearn.com'
ON CONFLICT (student_id, topic) DO NOTHING;

INSERT INTO topic_progress (student_id, topic, mastery_score, attempts, correct_count, p_know)
SELECT sp.id, 'substitution', 83.3, 12, 10, 0.85
FROM student_profiles sp JOIN users u ON sp.user_id = u.id WHERE u.email = 'bruce@neurolearn.com'
ON CONFLICT (student_id, topic) DO NOTHING;

INSERT INTO topic_progress (student_id, topic, mastery_score, attempts, correct_count, p_know)
SELECT sp.id, 'elimination', 90.0, 10, 9, 0.91
FROM student_profiles sp JOIN users u ON sp.user_id = u.id WHERE u.email = 'bruce@neurolearn.com'
ON CONFLICT (student_id, topic) DO NOTHING;

-- 4. Insert Topic Progress for Dick Grayson (Moderate Performer)
INSERT INTO topic_progress (student_id, topic, mastery_score, attempts, correct_count, p_know)
SELECT sp.id, 'intro', 75.0, 8, 6, 0.78
FROM student_profiles sp JOIN users u ON sp.user_id = u.id WHERE u.email = 'dick@neurolearn.com'
ON CONFLICT (student_id, topic) DO NOTHING;

INSERT INTO topic_progress (student_id, topic, mastery_score, attempts, correct_count, p_know)
SELECT sp.id, 'graphing', 71.4, 7, 5, 0.72
FROM student_profiles sp JOIN users u ON sp.user_id = u.id WHERE u.email = 'dick@neurolearn.com'
ON CONFLICT (student_id, topic) DO NOTHING;

INSERT INTO topic_progress (student_id, topic, mastery_score, attempts, correct_count, p_know)
SELECT sp.id, 'substitution', 60.0, 10, 6, 0.63
FROM student_profiles sp JOIN users u ON sp.user_id = u.id WHERE u.email = 'dick@neurolearn.com'
ON CONFLICT (student_id, topic) DO NOTHING;

-- 5. Insert Topic Progress for Jason Todd (Struggling - Triggers Bottleneck Alert on 'substitution')
INSERT INTO topic_progress (student_id, topic, mastery_score, attempts, correct_count, p_know)
SELECT sp.id, 'intro', 50.0, 6, 3, 0.45
FROM student_profiles sp JOIN users u ON sp.user_id = u.id WHERE u.email = 'jason@neurolearn.com'
ON CONFLICT (student_id, topic) DO NOTHING;

INSERT INTO topic_progress (student_id, topic, mastery_score, attempts, correct_count, p_know)
SELECT sp.id, 'substitution', 28.5, 7, 2, 0.26
FROM student_profiles sp JOIN users u ON sp.user_id = u.id WHERE u.email = 'jason@neurolearn.com'
ON CONFLICT (student_id, topic) DO NOTHING;

-- 6. Link Parent Martha Wayne to Bruce Wayne and Dick Grayson
INSERT INTO parent_student_links (parent_user_id, student_user_id, created_at)
SELECT p.id, s.id, CURRENT_TIMESTAMP
FROM users p, users s
WHERE p.email = 'parent@neurolearn.com' AND s.email = 'bruce@neurolearn.com'
ON CONFLICT (parent_user_id, student_user_id) DO NOTHING;

INSERT INTO parent_student_links (parent_user_id, student_user_id, created_at)
SELECT p.id, s.id, CURRENT_TIMESTAMP
FROM users p, users s
WHERE p.email = 'parent@neurolearn.com' AND s.email = 'dick@neurolearn.com'
ON CONFLICT (parent_user_id, student_user_id) DO NOTHING;

-- 7. Insert Historical Question Attempts for Bruce Wayne
INSERT INTO question_attempts (student_id, question_id, session_id, correct, response_time_ms, attempt_number, timestamp)
SELECT sp.id, 1, NULL, true, 4200, 1, CURRENT_TIMESTAMP - INTERVAL '2 hours'
FROM student_profiles sp JOIN users u ON sp.user_id = u.id WHERE u.email = 'bruce@neurolearn.com';

INSERT INTO question_attempts (student_id, question_id, session_id, correct, response_time_ms, attempt_number, timestamp)
SELECT sp.id, 2, NULL, true, 3800, 1, CURRENT_TIMESTAMP - INTERVAL '1 hour 45 minutes'
FROM student_profiles sp JOIN users u ON sp.user_id = u.id WHERE u.email = 'bruce@neurolearn.com';

INSERT INTO question_attempts (student_id, question_id, session_id, correct, response_time_ms, attempt_number, timestamp)
SELECT sp.id, 3, NULL, true, 5100, 1, CURRENT_TIMESTAMP - INTERVAL '1 hour 30 minutes'
FROM student_profiles sp JOIN users u ON sp.user_id = u.id WHERE u.email = 'bruce@neurolearn.com';

INSERT INTO question_attempts (student_id, question_id, session_id, correct, response_time_ms, attempt_number, timestamp)
SELECT sp.id, 4, NULL, false, 8200, 1, CURRENT_TIMESTAMP - INTERVAL '1 hour'
FROM student_profiles sp JOIN users u ON sp.user_id = u.id WHERE u.email = 'bruce@neurolearn.com';

INSERT INTO question_attempts (student_id, question_id, session_id, correct, response_time_ms, attempt_number, timestamp)
SELECT sp.id, 4, NULL, true, 4100, 2, CURRENT_TIMESTAMP - INTERVAL '50 minutes'
FROM student_profiles sp JOIN users u ON sp.user_id = u.id WHERE u.email = 'bruce@neurolearn.com';
