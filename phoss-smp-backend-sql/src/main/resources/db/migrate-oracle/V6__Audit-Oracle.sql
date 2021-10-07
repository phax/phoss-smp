--
-- Copyright (C) 2019-2021 Philip Helger and contributors
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

CREATE TABLE smp_audit (
  id         int          NOT NULL AUTO_INCREMENT,
  dt         timestamp    NOT NULL,
  userid     varchar(20)  NOT NULL,
  actiontype varchar(10)  NOT NULL,
  success    boolean      NOT NULL,
  action     text
  CONSTRAINT smp_audit_pk PRIMARY KEY (id) USING INDEX tablespace USERS
);

COMMENT ON COLUMN smp_audit.dt         IS 'Internal ID';
COMMENT ON COLUMN smp_audit.userid     IS 'The executing user ID';
COMMENT ON COLUMN smp_audit.actiontype IS 'Transport profile name';
COMMENT ON COLUMN smp_audit.success    IS 'Was the action successful or not?';
COMMENT ON COLUMN smp_audit.action     IS 'The action and arguments that were performed';
