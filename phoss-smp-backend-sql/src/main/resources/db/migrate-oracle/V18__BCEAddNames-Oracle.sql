ALTER TABLE smp_bce ADD COLUMN names clob DEFAULT NULL;
-- Make column nullable
ALTER TABLE smp_bce MODIFY (name NULL);
