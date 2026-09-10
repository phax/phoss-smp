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

CREATE TABLE `smp_sectotp` (
  `userid`   varchar(45)  NOT NULL COMMENT 'The ID of the user this enrollment belongs to',
  `secret`   varchar(128) NOT NULL COMMENT 'The Base32 encoded shared secret',
  `enabled`  tinyint(1)   NOT NULL COMMENT 'Was the enrollment confirmed by the user?',
  `regdt`    datetime     NOT NULL COMMENT 'The date and time the enrollment was created',
  `lastslot` bigint                COMMENT 'The last successfully used TOTP time slot',
  PRIMARY KEY (`userid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='SMP User TOTP enrollments';
