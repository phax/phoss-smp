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
  endpointReference VARCHAR(256) NULL,
  certificate       varchar(max) NULL,
  CONSTRAINT PK_smp_access_point PRIMARY KEY (id)
);
GO

CREATE UNIQUE INDEX UX_smp_access_point_name ON smp_access_point (name);
GO

ALTER TABLE smp_endpoint ALTER COLUMN endpointReference VARCHAR(256) NULL;
GO

ALTER TABLE smp_endpoint ALTER COLUMN certificate varchar(max) NULL;
GO

ALTER TABLE smp_endpoint ADD accessPointID VARCHAR(45) NULL;
GO

CREATE INDEX IX_smp_endpoint_accessPointID ON smp_endpoint (accessPointID);
GO

UPDATE STATISTICS smp_endpoint;
GO

UPDATE STATISTICS smp_access_point;
GO
