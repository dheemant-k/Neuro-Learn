-- V10__question_review_and_provenance.sql
-- Teacher-reviewed, LLM-assisted question generation (FUTURE_WORKFLOW Phase 4b).
-- Students are only ever served APPROVED questions; generated ones start as PENDING.
-- Every existing (seeded) question becomes APPROVED / SEED.

ALTER TABLE questions
    ADD COLUMN status             VARCHAR(10) NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN source             VARCHAR(10) NOT NULL DEFAULT 'SEED',
    ADD COLUMN created_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN generator_model    VARCHAR(60),
    ADD COLUMN prompt_version     VARCHAR(20),
    ADD COLUMN created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN reviewed_at        TIMESTAMP WITH TIME ZONE,
    ADD CONSTRAINT chk_question_status CHECK (status IN ('APPROVED', 'PENDING', 'REJECTED')),
    ADD CONSTRAINT chk_question_source CHECK (source IN ('SEED', 'LLM', 'TEACHER'));

CREATE INDEX idx_questions_status_topic ON questions(status, topic);
CREATE INDEX idx_questions_created_by ON questions(created_by_user_id, created_at);

-- The student mistake each wrong option represents (feeds hints and teacher analytics later).
CREATE TABLE question_misconceptions (
    id            BIGSERIAL PRIMARY KEY,
    question_id   BIGINT     NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    option_key    VARCHAR(1) NOT NULL CHECK (option_key IN ('A', 'B', 'C', 'D')),
    misconception TEXT       NOT NULL,
    CONSTRAINT uk_question_option_misconception UNIQUE (question_id, option_key)
);
