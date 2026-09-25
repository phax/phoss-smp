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

CREATE TABLE `smp_access_point` (
  `id`                VARCHAR(45)  NOT NULL,
  `name`              VARCHAR(64)  NOT NULL,
  `endpointReference` VARCHAR(256) DEFAULT NULL,
  `certificate`       LONGTEXT     DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `smp_access_point` ADD UNIQUE KEY `UX_smp_access_point_name` (`name`);

ALTER TABLE `smp_endpoint`
  MODIFY COLUMN `endpointReference` VARCHAR(256) DEFAULT NULL,
  MODIFY COLUMN `certificate` LONGTEXT DEFAULT NULL;

ALTER TABLE `smp_endpoint` ADD COLUMN `accessPointID` VARCHAR(45) DEFAULT NULL;
ALTER TABLE `smp_endpoint` ADD INDEX `IX_smp_endpoint_accessPointID` (`accessPointID`);

ANALYZE TABLE `smp_endpoint`;
ANALYZE TABLE `smp_access_point`;
