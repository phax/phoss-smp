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

CREATE TABLE smp_sectotp (
  userid   varchar(45)  NOT NULL,
  secret   varchar(128) NOT NULL,
  enabled  boolean      NOT NULL,
  regdt    timestamp    NOT NULL,
  lastslot bigint,
  PRIMARY KEY (userid)
);

COMMENT ON TABLE  smp_sectotp          IS 'SMP User TOTP enrollments';
COMMENT ON COLUMN smp_sectotp.userid   IS 'The ID of the user this enrollment belongs to';
COMMENT ON COLUMN smp_sectotp.secret   IS 'The Base32 encoded shared secret';
COMMENT ON COLUMN smp_sectotp.enabled  IS 'Was the enrollment confirmed by the user?';
COMMENT ON COLUMN smp_sectotp.regdt    IS 'The date and time the enrollment was created';
COMMENT ON COLUMN smp_sectotp.lastslot IS 'The last successfully used TOTP time slot';
