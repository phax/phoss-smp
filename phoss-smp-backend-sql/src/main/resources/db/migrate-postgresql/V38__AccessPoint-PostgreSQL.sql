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

-- Step 1: Create the new Access Point table.
-- An Access Point is identified by its endpoint reference URL, because a physical
-- Access Point can technically only have one single public certificate.
-- Note: the UNIQUE index on endpointReference is created after the initial data
-- load (step 4), because bulk loading into an unindexed table is significantly
-- faster.
CREATE TABLE smp_access_point (
  id                varchar(45)  NOT NULL,
  endpointReference varchar(256) DEFAULT NULL,
  certificate       text         DEFAULT NULL,
  PRIMARY KEY (id)
);

-- Step 2: Add the reference column to the endpoint table
ALTER TABLE smp_endpoint ADD COLUMN accessPointID varchar(45) DEFAULT NULL;

-- Step 3: Temporary index on the column to be de-duplicated.
-- Without it, the grouping in step 4 and the join in step 5 degrade into
-- (repeated) full table scans, which is prohibitively slow for large endpoint
-- tables. It is dropped again in step 6.
CREATE INDEX IX_smp_endpoint_epr_tmp ON smp_endpoint (endpointReference);

-- Step 4: Create one Access Point per distinct endpointReference.
-- The ID of the first endpoint of each group is reused as the Access Point ID,
-- because it is unique by definition. The certificate of that very same endpoint
-- is used as the certificate of the Access Point.
-- The distinct URLs are determined with a single grouped pass over the index of
-- step 3 and are then joined back via the primary key, so that the certificate
-- (a potentially large value) is only read for the rows that are inserted.
INSERT INTO smp_access_point (id, endpointReference, certificate)
  SELECT e.id, e.endpointReference, e.certificate
  FROM smp_endpoint e
  INNER JOIN (SELECT MIN(id) AS id
              FROM smp_endpoint
              GROUP BY endpointReference) g ON g.id = e.id;

CREATE UNIQUE INDEX UX_smp_access_point_epr ON smp_access_point (endpointReference);

-- Step 5: Point all endpoints to their Access Point.
-- A set based join is used instead of a correlated subquery, so that the
-- optimizer can use a single hash/merge join instead of one index lookup per
-- endpoint row.
UPDATE smp_endpoint e
  SET accessPointID = ap.id
  FROM smp_access_point ap
  WHERE ap.endpointReference = e.endpointReference;

-- Step 6: Drop the now redundant columns in a single statement, so that the
-- table is processed only once
DROP INDEX IX_smp_endpoint_epr_tmp;
ALTER TABLE smp_endpoint
  DROP COLUMN endpointReference,
  DROP COLUMN certificate;

-- Step 7: Index for the join. Created last, because maintaining it during the
-- mass update of step 5 would be pure overhead.
CREATE INDEX IX_smp_endpoint_accessPointID ON smp_endpoint (accessPointID);

-- Step 8: Refresh the statistics, because the table layout changed considerably
ANALYZE smp_endpoint;
ANALYZE smp_access_point;
