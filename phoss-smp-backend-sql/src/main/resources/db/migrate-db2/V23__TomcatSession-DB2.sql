--
-- Copyright (C) 2019-2025 Philip Helger and contributors
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

CREATE TABLE tomcat_sessions (
  session_id     varchar(100) NOT NULL,
  valid_session  char(1)      NOT NULL DEFAULT '1',
  max_inactive   intege       NOT NULL,
  last_access    bigint       NOT NULL,
  app_name       varchar(255),
  session_data   blob(16M),
  CONSTRAINT pk_tomcat_sessions PRIMARY KEY (session_id)
);

CREATE INDEX idx_app_name ON tomcat_sessions (app_name ASC);

COMMENT ON TABLE tomcat_sessions IS 'Tomcat Persistent Session Storage';
