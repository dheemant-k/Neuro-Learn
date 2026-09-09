-- V1__init_schema.sql: Core schema migration for NeuroLearn

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS student_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    current_level INT DEFAULT 1,
    overall_accuracy DOUBLE PRECISION DEFAULT 0.0,
    total_xp INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS questions (
    id BIGSERIAL PRIMARY KEY,
    topic VARCHAR(100) NOT NULL,
    difficulty INT NOT NULL DEFAULT 1,
    question_text TEXT NOT NULL,
    option_a TEXT NOT NULL,
    option_b TEXT NOT NULL,
    option_c TEXT NOT NULL,
    option_d TEXT NOT NULL,
    correct_answer VARCHAR(10) NOT NULL,
    explanation TEXT,
    subject VARCHAR(50) DEFAULT 'Math'
);

CREATE TABLE IF NOT EXISTS learning_sessions (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    start_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP WITH TIME ZONE,
    questions_attempted INT DEFAULT 0,
    accuracy DOUBLE PRECISION DEFAULT 0.0
);

CREATE TABLE IF NOT EXISTS question_attempts (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    session_id BIGINT REFERENCES learning_sessions(id) ON DELETE SET NULL,
    correct BOOLEAN NOT NULL,
    response_time_ms BIGINT NOT NULL DEFAULT 0,
    attempt_number INT DEFAULT 1,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS topic_progress (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    topic VARCHAR(100) NOT NULL,
    mastery_score DOUBLE PRECISION DEFAULT 0.0,
    attempts INT DEFAULT 0,
    correct_count INT DEFAULT 0,
    CONSTRAINT uk_student_topic UNIQUE (student_id, topic)
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_questions_topic_diff ON questions(topic, difficulty);
CREATE INDEX idx_attempts_student ON question_attempts(student_id);
CREATE INDEX idx_sessions_student ON learning_sessions(student_id);
