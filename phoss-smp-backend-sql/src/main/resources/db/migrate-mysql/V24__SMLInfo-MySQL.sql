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

CREATE TABLE `smp_sml_info` (
  `id`               varchar(45)  NOT NULL COMMENT 'Internal ID',
  `displayname`      varchar(256) NOT NULL COMMENT 'Display name',
  `dnszone`          varchar(256) NOT NULL COMMENT 'DNS zone',
  `serviceurl`       varchar(500) NOT NULL COMMENT 'Management service URL',
  `managesmp`        varchar(256) NOT NULL COMMENT 'URL suffix for managing SMPs',
  `manageparticipant` varchar(256) NOT NULL COMMENT 'URL suffix for managing participants',
  `clientcert`       tinyint(1)   NOT NULL COMMENT 'Client certificate required?',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='SMP SML Information';
