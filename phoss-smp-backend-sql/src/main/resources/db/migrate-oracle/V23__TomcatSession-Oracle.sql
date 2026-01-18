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
  session_id     varchar(100)  NOT NULL,
  -- DEFAULT must be before NOT NULL
  valid_session  char(1)       DEFAULT '1' NOT NULL,
  max_inactive   number(10)    NOT NULL,
  last_access    number(19)    NOT NULL,
  app_name       varchar(255),
  session_data   blob,
  CONSTRAINT pk_tomcat_sessions PRIMARY KEY (session_id) USING INDEX tablespace USERS
);

CREATE INDEX idx_app_name ON tomcat_sessions (app_name);

COMMENT ON TABLE tomcat_sessions IS 'Tomcat Persistent Session Storage';
