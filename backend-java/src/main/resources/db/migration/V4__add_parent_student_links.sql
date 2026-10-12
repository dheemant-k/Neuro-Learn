-- V4__add_parent_student_links.sql: Parent-student relationship tracking
CREATE TABLE IF NOT EXISTS parent_student_links (
    id BIGSERIAL PRIMARY KEY,
    parent_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    student_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_parent_student UNIQUE(parent_user_id, student_user_id)
);

CREATE INDEX idx_parent_student_parent ON parent_student_links(parent_user_id);
CREATE INDEX idx_parent_student_student ON parent_student_links(student_user_id);
