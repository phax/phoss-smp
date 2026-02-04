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

-- Drop foreign key constraints (required for MariaDB)
-- Thanks to Uresha for providing this block and the last block
ALTER TABLE `smp_ownership`             DROP FOREIGN KEY `FK_smp_ownership_id`;
ALTER TABLE `smp_service_metadata_red`  DROP FOREIGN KEY `FK_smp_redirect_businessIdentifier`;
ALTER TABLE `smp_service_metadata`      DROP FOREIGN KEY `FK_smp_service_metadata_businessIdentifier`;
ALTER TABLE `smp_process`               DROP FOREIGN KEY `FK_smp_process_documentIdentifierScheme`;
ALTER TABLE `smp_endpoint`              DROP FOREIGN KEY `FK_smp_endpoint_documentIdentifierScheme`;

-- Change type from "varchar(50)" to "varchar(135)"
ALTER TABLE `smp_service_group`        MODIFY `businessIdentifier` varchar(135) NOT NULL;
ALTER TABLE `smp_service_metadata`     MODIFY `businessIdentifier` varchar(135) NOT NULL;
ALTER TABLE `smp_process`              MODIFY `businessIdentifier` varchar(135) NOT NULL;
ALTER TABLE `smp_endpoint`             MODIFY `businessIdentifier` varchar(135) NOT NULL;
ALTER TABLE `smp_ownership`            MODIFY `businessIdentifier` varchar(135) NOT NULL;
ALTER TABLE `smp_service_metadata_red` MODIFY `businessIdentifier` varchar(135) NOT NULL;

-- Adding the dropped foreign key constraints back (required for MariaDB)
ALTER TABLE `smp_ownership`             ADD CONSTRAINT `FK_smp_ownership_id`                        FOREIGN KEY (`businessIdentifierScheme`, `businessIdentifier`)                                                                                                  REFERENCES `smp_service_group` (`businessIdentifierScheme`, `businessIdentifier`)                                                                                           ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `smp_service_metadata_red`  ADD CONSTRAINT `FK_smp_redirect_businessIdentifier`         FOREIGN KEY (`businessIdentifierScheme`, `businessIdentifier`)                                                                                                  REFERENCES `smp_service_group` (`businessIdentifierScheme`, `businessIdentifier`)                                                                                           ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `smp_service_metadata`      ADD CONSTRAINT `FK_smp_service_metadata_businessIdentifier` FOREIGN KEY (`businessIdentifierScheme`, `businessIdentifier`)                                                                                                  REFERENCES `smp_service_group` (`businessIdentifierScheme`, `businessIdentifier`)                                                                                           ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `smp_process`               ADD CONSTRAINT `FK_smp_process_documentIdentifierScheme`    FOREIGN KEY (`businessIdentifierScheme`, `businessIdentifier`, `documentIdentifierScheme`, `documentIdentifier`)                                                REFERENCES `smp_service_metadata` (`businessIdentifierScheme`, `businessIdentifier`, `documentIdentifierScheme`, `documentIdentifier`)                                      ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `smp_endpoint`              ADD CONSTRAINT `FK_smp_endpoint_documentIdentifierScheme`   FOREIGN KEY (`businessIdentifierScheme`, `businessIdentifier`, `documentIdentifierScheme`, `documentIdentifier`, `processIdentifierType`, `processIdentifier`)  REFERENCES `smp_process` (`businessIdentifierScheme`, `businessIdentifier`, `documentIdentifierScheme`, `documentIdentifier`, `processIdentifierType`, `processIdentifier`) ON DELETE CASCADE ON UPDATE CASCADE;
