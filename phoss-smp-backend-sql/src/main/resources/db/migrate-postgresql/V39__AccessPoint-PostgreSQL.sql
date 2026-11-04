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
  id                varchar(45)  NOT NULL,
  name              varchar(64)  NOT NULL,
  endpointReference varchar(256) DEFAULT NULL,
  certificate       text         DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE UNIQUE INDEX UX_smp_access_point_name ON smp_access_point (name);

ALTER TABLE smp_endpoint
  ALTER COLUMN endpointReference DROP NOT NULL,
  ALTER COLUMN certificate DROP NOT NULL;

ALTER TABLE smp_endpoint ADD COLUMN accessPointID varchar(45) DEFAULT NULL;
CREATE INDEX IX_smp_endpoint_accessPointID ON smp_endpoint (accessPointID);

ANALYZE smp_endpoint;
ANALYZE smp_access_point;
