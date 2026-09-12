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
  id                VARCHAR(45)  NOT NULL,
  name              VARCHAR(64)  NOT NULL,
  endpointReference VARCHAR(256),
  certificate       CLOB,
  CONSTRAINT pk_smp_access_point PRIMARY KEY (id)
);

CREATE UNIQUE INDEX UX_smp_access_point_name ON smp_access_point (name);

ALTER TABLE smp_endpoint ALTER COLUMN endpointReference DROP NOT NULL;
ALTER TABLE smp_endpoint ALTER COLUMN certificate DROP NOT NULL;
CALL SYSPROC.ADMIN_CMD('REORG TABLE smp_endpoint');

ALTER TABLE smp_endpoint ADD COLUMN accessPointID VARCHAR(45);
CREATE INDEX IX_smp_endpoint_apid ON smp_endpoint (accessPointID);

CALL SYSPROC.ADMIN_CMD('RUNSTATS ON TABLE smp_endpoint WITH DISTRIBUTION AND DETAILED INDEXES ALL');
CALL SYSPROC.ADMIN_CMD('RUNSTATS ON TABLE smp_access_point WITH DISTRIBUTION AND DETAILED INDEXES ALL');
