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

CREATE TABLE smp_sml_info (
  id                varchar(45)  NOT NULL,
  displayname       clob         NOT NULL,
  dnszone           varchar(256) NOT NULL,
  serviceurl        varchar(500) NOT NULL,
  managesmp         varchar(256) NOT NULL,
  manageparticipant varchar(256) NOT NULL,
  clientcert        number(1)    NOT NULL,
  CONSTRAINT smp_sml_info_pk PRIMARY KEY (id) USING INDEX tablespace USERS
);

COMMENT ON COLUMN smp_sml_info.id                IS 'Internal ID';
COMMENT ON COLUMN smp_sml_info.displayname       IS 'Display name';
COMMENT ON COLUMN smp_sml_info.dnszone           IS 'DNS zone';
COMMENT ON COLUMN smp_sml_info.serviceurl        IS 'Management service URL';
COMMENT ON COLUMN smp_sml_info.managesmp         IS 'URL suffix for managing SMPs';
COMMENT ON COLUMN smp_sml_info.manageparticipant IS 'URL suffix for managing participants';
COMMENT ON COLUMN smp_sml_info.clientcert        IS 'Client certificate required?';
