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

-- Change type from "varchar(20)" to "varchar(45)"
ALTER TABLE smp_secrole      ALTER COLUMN creationuserid TYPE varchar(45);
ALTER TABLE smp_secrole      ALTER COLUMN lastmoduserid  TYPE varchar(45);
ALTER TABLE smp_secrole      ALTER COLUMN deleteuserid   TYPE varchar(45);
ALTER TABLE smp_secuser      ALTER COLUMN creationuserid TYPE varchar(45);
ALTER TABLE smp_secuser      ALTER COLUMN lastmoduserid  TYPE varchar(45);
ALTER TABLE smp_secuser      ALTER COLUMN deleteuserid   TYPE varchar(45);
ALTER TABLE smp_secusergroup ALTER COLUMN creationuserid TYPE varchar(45);
ALTER TABLE smp_secusergroup ALTER COLUMN lastmoduserid  TYPE varchar(45);
ALTER TABLE smp_secusergroup ALTER COLUMN deleteuserid   TYPE varchar(45);
ALTER TABLE smp_secusertoken ALTER COLUMN creationuserid TYPE varchar(45);
ALTER TABLE smp_secusertoken ALTER COLUMN lastmoduserid  TYPE varchar(45);
ALTER TABLE smp_secusertoken ALTER COLUMN deleteuserid   TYPE varchar(45);

-- The executing user ID of an audit item is a user ID as well
ALTER TABLE smp_audit        ALTER COLUMN userid         TYPE varchar(45);
