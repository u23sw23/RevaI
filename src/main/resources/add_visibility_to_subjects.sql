

USE demo2;

ALTER TABLE subjects 
ADD COLUMN IF NOT EXISTS visibility VARCHAR(10) NOT NULL DEFAULT 'private' AFTER user_id;

CREATE INDEX IF NOT EXISTS idx_visibility ON subjects(visibility);

