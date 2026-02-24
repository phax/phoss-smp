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

CREATE TABLE `smp_audit` (
  `id`         int          NOT NULL AUTO_INCREMENT COMMENT 'Ensure order of entry',
  `dt`         datetime     NOT NULL                COMMENT 'The date and time of the execution',
  `userid`     varchar(20)  NOT NULL                COMMENT 'The executing user ID',
  `actiontype` varchar(10)  NOT NULL                COMMENT 'The object type',
  `success`    tinyint(1)   NOT NULL                COMMENT 'Was the action successful or not?',
  `action`     text                                 COMMENT 'The action and arguments that were performed',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='SMP Audit';
