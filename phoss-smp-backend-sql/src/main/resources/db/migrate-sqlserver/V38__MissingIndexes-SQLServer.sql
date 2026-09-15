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
-- added them manually are not broken by this migration

-- smp_ownership: the FK to smp_user is unindexed, and "username" is the trailing PK column
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_smp_ownership_username' AND object_id = OBJECT_ID ('smp_ownership'))
  CREATE INDEX IX_smp_ownership_username ON smp_ownership (username);
GO

-- smp_service_metadata_red: the PK starts with "documentIdentifierScheme", so lookups by service group alone cannot use it
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_smp_smr_participant' AND object_id = OBJECT_ID ('smp_service_metadata_red'))
  CREATE INDEX IX_smp_smr_participant ON smp_service_metadata_red (businessIdentifierScheme, businessIdentifier);
GO

-- smp_endpoint: the transport profile usage check counts rows by "transportProfile"
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_smp_endpoint_tprofile' AND object_id = OBJECT_ID ('smp_endpoint'))
  CREATE INDEX IX_smp_endpoint_tprofile ON smp_endpoint (transportProfile);
GO

-- smp_pmigration: listed by direction [and state], deleted by "pid"
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_smp_pmigration_dir_state' AND object_id = OBJECT_ID ('smp_pmigration'))
  CREATE INDEX IX_smp_pmigration_dir_state ON smp_pmigration (direction, state);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_smp_pmigration_pid' AND object_id = OBJECT_ID ('smp_pmigration'))
  CREATE INDEX IX_smp_pmigration_pid ON smp_pmigration (pid);
GO

-- smp_audit: grows unbounded, is listed ordered by "dt" and filtered by "userid"
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_smp_audit_dt' AND object_id = OBJECT_ID ('smp_audit'))
  CREATE INDEX IX_smp_audit_dt ON smp_audit (dt);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_smp_audit_userid' AND object_id = OBJECT_ID ('smp_audit'))
  CREATE INDEX IX_smp_audit_userid ON smp_audit (userid);
GO

-- smp_secuser: the login path also resolves users by email; the login name is indexed since V8
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_smp_secuser_email' AND object_id = OBJECT_ID ('smp_secuser'))
  CREATE INDEX IX_smp_secuser_email ON smp_secuser (email);
GO

-- smp_secusertoken: token to user resolution
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_smp_secusertoken_userid' AND object_id = OBJECT_ID ('smp_secusertoken'))
  CREATE INDEX IX_smp_secusertoken_userid ON smp_secusertoken (userid);
GO
