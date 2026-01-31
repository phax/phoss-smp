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

CREATE TABLE smp_pmigration (
  id        varchar(45)  NOT NULL,
  direction varchar(45)  NOT NULL,
  state     varchar(45)  NOT NULL,
  pid       varchar(255) NOT NULL,
  initdt    timestamp    NOT NULL,
  migkey    varchar(45)  NOT NULL,
  constraint smp_pmigration_pk PRIMARY KEY (id)  using index tablespace USERS
) tablespace USERS;


COMMENT ON COLUMN smp_pmigration.id        IS 'Internal ID';
COMMENT ON COLUMN smp_pmigration.direction IS 'Migration direction';
COMMENT ON COLUMN smp_pmigration.state     IS 'Migration state';
COMMENT ON COLUMN smp_pmigration.pid       IS 'Participant/Business ID';
COMMENT ON COLUMN smp_pmigration.initdt    IS 'The date and time when the migration was initiated';
COMMENT ON COLUMN smp_pmigration.migkey    IS 'The migration key itself';   
