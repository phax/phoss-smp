--
-- Copyright (C) 2019-2026 Philip Helger and contributors
-- philip[at]helger[dot]com
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--         http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

-- smp_ownership (username), smp_bce (pid) and smp_service_metadata_red (businessIdentifierScheme, businessIdentifier)
-- are already indexed since V1 as the FK indexes InnoDB requires

-- All indexes are created conditionally, so that installations that already
-- added them manually are not broken by this migration.
-- MySQL has no "CREATE INDEX IF NOT EXISTS", so a temporary helper procedure
-- checks INFORMATION_SCHEMA before creating each index
DROP PROCEDURE IF EXISTS smp_create_index;

CREATE PROCEDURE smp_create_index (IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_columns VARCHAR(512))
BEGIN
  DECLARE n_existing INT;
  SELECT COUNT(*) INTO n_existing
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = p_table AND index_name = p_index;
  IF n_existing = 0 THEN
    SET @smp_create_index_sql = CONCAT ('CREATE INDEX `', p_index, '` ON `', p_table, '` (', p_columns, ')');
    PREPARE smp_create_index_stmt FROM @smp_create_index_sql;
    EXECUTE smp_create_index_stmt;
    DEALLOCATE PREPARE smp_create_index_stmt;
  END IF;
END;

-- smp_endpoint: the transport profile usage check counts rows by "transportProfile"
CALL smp_create_index ('smp_endpoint', 'IX_smp_endpoint_tprofile', '`transportProfile`');

-- smp_pmigration: listed by direction [and state], deleted by "pid"
CALL smp_create_index ('smp_pmigration', 'IX_smp_pmigration_dir_state', '`direction`, `state`');
CALL smp_create_index ('smp_pmigration', 'IX_smp_pmigration_pid', '`pid`');

-- smp_audit: grows unbounded, is listed ordered by "dt" and filtered by "userid"
CALL smp_create_index ('smp_audit', 'IX_smp_audit_dt', '`dt`');
CALL smp_create_index ('smp_audit', 'IX_smp_audit_userid', '`userid`');

-- smp_secuser: the login path resolves users by login name and by email
-- Both columns are TEXT here, so they are indexed by prefix
-- DB2 and SQL Server have the login name index since V8, under their own name
CALL smp_create_index ('smp_secuser', 'IX_smp_secuser_loginname', '`loginname`(191)');
CALL smp_create_index ('smp_secuser', 'IX_smp_secuser_email', '`email`(191)');

-- smp_secusertoken: token to user resolution
CALL smp_create_index ('smp_secusertoken', 'IX_smp_secusertoken_userid', '`userid`');

DROP PROCEDURE smp_create_index;
