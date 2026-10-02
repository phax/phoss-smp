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

CREATE TABLE smp_access_point (
  id                VARCHAR2(45)  NOT NULL,
  name              VARCHAR2(64)  NOT NULL,
  endpointReference VARCHAR2(256),
  certificate       CLOB,
  CONSTRAINT pk_smp_access_point PRIMARY KEY (id)
);

CREATE UNIQUE INDEX UX_smp_access_point_name ON smp_access_point (name);

ALTER TABLE smp_endpoint MODIFY (endpointReference NULL, certificate NULL);

ALTER TABLE smp_endpoint ADD accessPointID VARCHAR2(45);
CREATE INDEX IX_smp_endpoint_apid ON smp_endpoint (accessPointID);

-- Note: no explicit statistics gathering is done here, because Oracle refreshes
-- the statistics of both tables with its automatic optimizer statistics task.
