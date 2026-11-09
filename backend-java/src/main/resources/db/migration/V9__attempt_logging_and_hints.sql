-- V9__attempt_logging_and_hints.sql
-- Data foundation for analytics/AI (docs/AI_FUTURE_SCOPE.md, Tier 0):
--   * which option the student picked (graded on the server from now on)
--   * how many hints they used on that question
--   * which adaptive decision served the question, and at what difficulty
-- Rows from before V9 keep NULLs / 0 in the new columns.

ALTER TABLE question_attempts
    ADD COLUMN selected_option   VARCHAR(1),
    ADD COLUMN hints_used        INT NOT NULL DEFAULT 0,
    ADD COLUMN adaptive_action   VARCHAR(10),
    ADD COLUMN adaptive_reason   TEXT,
    ADD COLUMN difficulty_served INT,
    ADD CONSTRAINT chk_attempt_selected_option CHECK (selected_option IN ('A', 'B', 'C', 'D'));

-- The decision behind the most recent question the engine served in a session,
-- copied onto the attempt when the student answers that question.
ALTER TABLE learning_sessions
    ADD COLUMN last_question_id     BIGINT REFERENCES questions(id) ON DELETE SET NULL,
    ADD COLUMN last_adaptive_action VARCHAR(10),
    ADD COLUMN last_adaptive_reason TEXT;

-- One row per hint/assist used on a question within a session.
CREATE TABLE session_hint_events (
    id                  BIGSERIAL PRIMARY KEY,
    session_id          BIGINT NOT NULL REFERENCES learning_sessions(id) ON DELETE CASCADE,
    question_id         BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    hint_type           VARCHAR(20) NOT NULL CHECK (hint_type IN ('FIFTY_FIFTY', 'CLUE')),
    eliminated_options  VARCHAR(8),  -- e.g. 'B,D' for FIFTY_FIFTY
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_hint_once_per_question UNIQUE (session_id, question_id, hint_type)
);

CREATE INDEX idx_hint_events_session_question ON session_hint_events(session_id, question_id);
