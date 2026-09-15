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

-- All indexes are created conditionally, so that installations that already
-- added them manually are not broken by this migration.
-- DB2 has no "CREATE INDEX IF NOT EXISTS", so SYSCAT.INDEXES is checked instead
BEGIN
  -- smp_ownership: the FK to smp_user is unindexed, and "username" is the trailing PK column
  IF NOT EXISTS (SELECT 1 FROM syscat.indexes WHERE indschema = CURRENT SCHEMA AND indname = 'IX_SMP_OWNERSHIP_USERNAME') THEN
    EXECUTE IMMEDIATE 'CREATE INDEX IX_smp_ownership_username ON smp_ownership (username)';
  END IF;

  -- smp_service_metadata_red: the PK starts with "documentIdentifierScheme", so lookups by service group alone cannot use it
  IF NOT EXISTS (SELECT 1 FROM syscat.indexes WHERE indschema = CURRENT SCHEMA AND indname = 'IX_SMP_SMR_PARTICIPANT') THEN
    EXECUTE IMMEDIATE 'CREATE INDEX IX_smp_smr_participant ON smp_service_metadata_red (businessIdentifierScheme, businessIdentifier)';
  END IF;

  -- smp_endpoint: the transport profile usage check counts rows by "transportProfile"
  IF NOT EXISTS (SELECT 1 FROM syscat.indexes WHERE indschema = CURRENT SCHEMA AND indname = 'IX_SMP_ENDPOINT_TPROFILE') THEN
    EXECUTE IMMEDIATE 'CREATE INDEX IX_smp_endpoint_tprofile ON smp_endpoint (transportProfile)';
  END IF;

  -- smp_pmigration: listed by direction [and state], deleted by "pid"
  IF NOT EXISTS (SELECT 1 FROM syscat.indexes WHERE indschema = CURRENT SCHEMA AND indname = 'IX_SMP_PMIGRATION_DIR_STATE') THEN
    EXECUTE IMMEDIATE 'CREATE INDEX IX_smp_pmigration_dir_state ON smp_pmigration (direction, state)';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM syscat.indexes WHERE indschema = CURRENT SCHEMA AND indname = 'IX_SMP_PMIGRATION_PID') THEN
    EXECUTE IMMEDIATE 'CREATE INDEX IX_smp_pmigration_pid ON smp_pmigration (pid)';
  END IF;

  -- smp_audit: grows unbounded, is listed ordered by "dt" and filtered by "userid"
  IF NOT EXISTS (SELECT 1 FROM syscat.indexes WHERE indschema = CURRENT SCHEMA AND indname = 'IX_SMP_AUDIT_DT') THEN
    EXECUTE IMMEDIATE 'CREATE INDEX IX_smp_audit_dt ON smp_audit (dt)';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM syscat.indexes WHERE indschema = CURRENT SCHEMA AND indname = 'IX_SMP_AUDIT_USERID') THEN
    EXECUTE IMMEDIATE 'CREATE INDEX IX_smp_audit_userid ON smp_audit (userid)';
  END IF;

  -- smp_secuser: the login path also resolves users by email; the login name is indexed since V8
  IF NOT EXISTS (SELECT 1 FROM syscat.indexes WHERE indschema = CURRENT SCHEMA AND indname = 'IX_SMP_SECUSER_EMAIL') THEN
    EXECUTE IMMEDIATE 'CREATE INDEX IX_smp_secuser_email ON smp_secuser (email)';
  END IF;

  -- smp_secusertoken: token to user resolution
  IF NOT EXISTS (SELECT 1 FROM syscat.indexes WHERE indschema = CURRENT SCHEMA AND indname = 'IX_SMP_SECUSERTOKEN_USERID') THEN
    EXECUTE IMMEDIATE 'CREATE INDEX IX_smp_secusertoken_userid ON smp_secusertoken (userid)';
  END IF;
END
