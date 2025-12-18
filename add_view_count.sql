-- Add view_count column to jobs table
ALTER TABLE jobs ADD COLUMN view_count INT DEFAULT 0;

-- Update existing records to have 0 view_count
UPDATE jobs SET view_count = 0 WHERE view_count IS NULL;
