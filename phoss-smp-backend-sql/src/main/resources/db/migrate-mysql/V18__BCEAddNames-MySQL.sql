ALTER TABLE smp_bce ADD COLUMN names text AFTER name;
-- Drop NOT NULL
ALTER TABLE smp_bce MODIFY name text COMMENT 'Entity name';
