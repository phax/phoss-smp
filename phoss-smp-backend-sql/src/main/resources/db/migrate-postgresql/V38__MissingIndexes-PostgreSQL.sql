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
CREATE INDEX IF NOT EXISTS IX_smp_ownership_username ON smp_ownership (username);

-- smp_bce: the PK is "id" only, but Business Cards are always accessed by "pid"
-- DB2, MySQL and SQL Server have this index since V1
CREATE INDEX IF NOT EXISTS IX_smp_bce_pid ON smp_bce (pid);

-- smp_service_metadata_red: the PK starts with "documentIdentifierScheme", so lookups by service group alone cannot use it
CREATE INDEX IF NOT EXISTS IX_smp_smr_participant ON smp_service_metadata_red (businessIdentifierScheme, businessIdentifier);

-- smp_endpoint: the transport profile usage check counts rows by "transportProfile"
CREATE INDEX IF NOT EXISTS IX_smp_endpoint_tprofile ON smp_endpoint (transportProfile);

-- smp_pmigration: listed by direction [and state], deleted by "pid"
CREATE INDEX IF NOT EXISTS IX_smp_pmigration_dir_state ON smp_pmigration (direction, state);
CREATE INDEX IF NOT EXISTS IX_smp_pmigration_pid ON smp_pmigration (pid);

-- smp_audit: grows unbounded, is listed ordered by "dt" and filtered by "userid"
CREATE INDEX IF NOT EXISTS IX_smp_audit_dt ON smp_audit (dt);
CREATE INDEX IF NOT EXISTS IX_smp_audit_userid ON smp_audit (userid);

-- smp_secuser: the login path resolves users by login name and by email
-- DB2 and SQL Server have the login name index since V8
CREATE INDEX IF NOT EXISTS IX_smp_secuser_loginname ON smp_secuser (loginname);
CREATE INDEX IF NOT EXISTS IX_smp_secuser_email ON smp_secuser (email);

-- smp_secusertoken: token to user resolution
CREATE INDEX IF NOT EXISTS IX_smp_secusertoken_userid ON smp_secusertoken (userid);
