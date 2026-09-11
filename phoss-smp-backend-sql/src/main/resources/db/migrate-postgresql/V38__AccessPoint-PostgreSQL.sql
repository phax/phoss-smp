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

-- Step 1: Create the new Access Point table
CREATE TABLE smp_access_point (
  id                varchar(45)  NOT NULL,
  endpointReference varchar(256) DEFAULT NULL,
  certificate       text         DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE INDEX IX_smp_access_point_epr ON smp_access_point (endpointReference);

-- Step 2: Add the reference column to the endpoint table
ALTER TABLE smp_endpoint ADD COLUMN accessPointID varchar(45) DEFAULT NULL;

-- Step 3: Create one Access Point per distinct (endpointReference, certificate) pair.
-- The ID of the first endpoint of each group is reused as the Access Point ID,
-- because it is unique by definition.
INSERT INTO smp_access_point (id, endpointReference, certificate)
  SELECT e.id, e.endpointReference, e.certificate
  FROM smp_endpoint e
  WHERE e.id = (SELECT MIN(e2.id)
                FROM smp_endpoint e2
                WHERE e2.endpointReference = e.endpointReference
                  AND e2.certificate = e.certificate);

-- Step 4: Point all endpoints to their Access Point
UPDATE smp_endpoint e
  SET accessPointID = (SELECT MIN(ap.id)
                       FROM smp_access_point ap
                       WHERE ap.endpointReference = e.endpointReference
                         AND ap.certificate = e.certificate);

-- Step 5: Drop the now redundant columns
ALTER TABLE smp_endpoint DROP COLUMN endpointReference;
ALTER TABLE smp_endpoint DROP COLUMN certificate;

-- Step 6: Index for the join
CREATE INDEX IX_smp_endpoint_accessPointID ON smp_endpoint (accessPointID);
