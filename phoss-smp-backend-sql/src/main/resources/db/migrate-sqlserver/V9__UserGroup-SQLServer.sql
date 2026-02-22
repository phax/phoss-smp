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

CREATE TABLE smp_secusergroup (
    id             varchar(45)  NOT NULL,
    creationdt     datetime2,
    creationuserid varchar(20),
    lastmoddt      datetime2,
    lastmoduserid  varchar(20),
    deletedt       datetime2,
    deleteuserid   varchar(20),
    attrs          varchar(max),
    name           varchar(255) NOT NULL,
    description    varchar(max),
    userids        varchar(max),
    roleids        varchar(max),
    CONSTRAINT pk_smp_secusergroup PRIMARY KEY
      (id)
  );
