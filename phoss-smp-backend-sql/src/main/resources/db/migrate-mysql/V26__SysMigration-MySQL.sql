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

CREATE TABLE `smp_sys_migration` (
  `migration_id` varchar(256) NOT NULL COMMENT 'Migration ID',
  `execution_dt` datetime     NOT NULL COMMENT 'Execution date and time',
  `success`      tinyint(1)   NOT NULL COMMENT 'Success?',
  `error_msg`    text                  COMMENT 'Error message'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='System Migration Results';

CREATE INDEX idx_smp_sys_migration_mid ON `smp_sys_migration` (`migration_id`);
