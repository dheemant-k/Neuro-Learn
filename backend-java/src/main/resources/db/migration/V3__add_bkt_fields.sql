-- V3__add_bkt_fields.sql: Bayesian Knowledge Tracing (BKT) persistence
ALTER TABLE topic_progress 
    ADD COLUMN IF NOT EXISTS p_know DOUBLE PRECISION DEFAULT 0.20;

ALTER TABLE student_profiles 
    ADD COLUMN IF NOT EXISTS p_know DOUBLE PRECISION DEFAULT 0.20;

UPDATE topic_progress SET p_know = 0.20 WHERE p_know IS NULL;
UPDATE student_profiles SET p_know = 0.20 WHERE p_know IS NULL;
