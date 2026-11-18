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

-- Oracle stores an empty string as NULL, so all columns that may contain an empty string must be
-- nullable. The current nullability is checked first, because V24 creates the SML columns
-- nullable for new installations, and Oracle rejects a MODIFY to the state a column already has.
DECLARE
  PROCEDURE set_nullable (p_table IN VARCHAR2, p_column IN VARCHAR2, p_nullable IN VARCHAR2) IS
    v_nullable VARCHAR2(1);
  BEGIN
    SELECT nullable INTO v_nullable FROM all_tab_columns
      WHERE owner = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')
        AND table_name = UPPER(p_table)
        AND column_name = UPPER(p_column);
    IF v_nullable <> p_nullable THEN
      EXECUTE IMMEDIATE 'ALTER TABLE ' || p_table || ' MODIFY (' || p_column ||
                        CASE p_nullable WHEN 'Y' THEN ' NULL)' ELSE ' NOT NULL)' END;
    END IF;
  END;
BEGIN
  -- smp_sml_info: an empty URL suffix is allowed
  set_nullable ('smp_sml_info', 'managesmp', 'Y');
  set_nullable ('smp_sml_info', 'manageparticipant', 'Y');
  -- smp_endpoint: these elements may be empty in the REST API and the XML import
  set_nullable ('smp_endpoint', 'endpointReference', 'Y');
  set_nullable ('smp_endpoint', 'serviceDescription', 'Y');
  set_nullable ('smp_endpoint', 'technicalContactUrl', 'Y');
  set_nullable ('smp_endpoint', 'certificate', 'Y');
  -- smp_service_metadata_red: V19 recreated the column as CLOB without NOT NULL, unlike all other databases
  set_nullable ('smp_service_metadata_red', 'redirectionUrl', 'N');
END;
/
