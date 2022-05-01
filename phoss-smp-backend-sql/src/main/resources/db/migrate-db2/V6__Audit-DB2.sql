--
-- Copyright (C) 2019-2022 Philip Helger and contributors
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
    id         int         NOT NULL  GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1),
    dt         timestamp   NOT NULL,
    userid     varchar(20) NOT NULL,
    actiontype varchar(10) NOT NULL,
    success    SMALLINT    NOT NULL,
    action     CLOB,
    CONSTRAINT pk_smp_audit PRIMARY KEY
      (id)
  );
