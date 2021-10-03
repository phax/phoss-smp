--
-- Copyright (C) 2019-2021 Philip Helger and contributors
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
  creationdt     timestamp,
  creationuserid varchar(20),
  lastmoddt      timestamp,
  lastmoduserid  varchar(20),
  deletedt       timestamp,
  deleteuserid   varchar(20),
  attrs          text,
  name           varchar(255) NOT NULL,
  description    text,
  PRIMARY KEY (id)
);

CREATE TABLE smp_secusergroup_user (
  ugid   varchar(45) NOT NULL,
  userid varchar(45) NOT NULL,
  CONSTRAINT FK_smp_secusergroup_user_ugid   FOREIGN KEY (ugid)   REFERENCES smp_secusergroup (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT FK_smp_secusergroup_user_userid FOREIGN KEY (userid) REFERENCES smp_secuser (id)      ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE smp_secusergroup_role (
  ugid   varchar(45) NOT NULL,
  roleid varchar(45) NOT NULL,
  CONSTRAINT FK_smp_secusergroup_role_ugid   FOREIGN KEY (ugid)   REFERENCES smp_secusergroup (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT FK_smp_secusergroup_role_roleid FOREIGN KEY (roleid) REFERENCES smp_secrole (id)      ON DELETE CASCADE ON UPDATE CASCADE
);
