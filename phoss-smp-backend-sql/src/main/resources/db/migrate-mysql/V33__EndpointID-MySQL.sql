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

-- Step 1: Add the new id column and populate it
ALTER TABLE `smp_endpoint` ADD COLUMN `id` varchar(45) NOT NULL DEFAULT '' FIRST;
UPDATE `smp_endpoint` SET `id` = UUID() WHERE `id` = '';

-- Step 2: Drop the FK constraint that is blocking the PRIMARY KEY drop
ALTER TABLE `smp_endpoint` DROP FOREIGN KEY `FK_smp_endpoint_documentIdentifierScheme`;

-- Step 3: Now we can safely drop and replace the PRIMARY KEY
ALTER TABLE `smp_endpoint` DROP PRIMARY KEY;
ALTER TABLE `smp_endpoint` ADD PRIMARY KEY (`id`);

-- Step 4: Recreate the FK constraint (unchanged, still references smp_process)
ALTER TABLE `smp_endpoint` 
  ADD CONSTRAINT `FK_smp_endpoint_documentIdentifierScheme` 
  FOREIGN KEY (`businessIdentifierScheme`, `businessIdentifier`, `documentIdentifierScheme`, `documentIdentifier`, `processIdentifierType`, `processIdentifier`) 
  REFERENCES `smp_process` (`businessIdentifierScheme`, `businessIdentifier`, `documentIdentifierScheme`, `documentIdentifier`, `processIdentifierType`, `processIdentifier`) 
  ON DELETE CASCADE ON UPDATE CASCADE;

-- No need for the additional index, as this is already covered in the FK constraint
-- ALTER TABLE `smp_endpoint` ADD INDEX `IX_smp_endpoint_process` (`businessIdentifierScheme`, `businessIdentifier`, `documentIdentifierScheme`, `documentIdentifier`, `processIdentifierType`, `processIdentifier`);
