--
-- Copyright (C) 2019-2025 Philip Helger and contributors
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

-- Cannot directly MODIFY VARCHAR2(256) to CLOB in Oracle
-- Use this safe 4-step process

-- Step 1: Add temporary CLOB column
ALTER TABLE smp_service_metadata_red ADD (TEMP_CLOB CLOB);

-- Step 2: Copy data (VARCHAR2 converts automatically to CLOB)
UPDATE smp_service_metadata_red SET TEMP_CLOB = redirectionUrl;
COMMIT;

-- Step 3: Drop original column
ALTER TABLE smp_service_metadata_red DROP COLUMN redirectionUrl;

-- Step 4: Rename new column to original name
ALTER TABLE smp_service_metadata_red RENAME COLUMN TEMP_CLOB TO redirectionUrl;
