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

-- Drop foreign key constraints
ALTER TABLE smp_ownership             DROP CONSTRAINT fk_smp_ownership_id;
ALTER TABLE smp_service_metadata_red  DROP CONSTRAINT fk_smp_redirect_businessidentifier;
ALTER TABLE smp_service_metadata      DROP CONSTRAINT fk_smp_service_metadata_businessidentifier;
ALTER TABLE smp_process               DROP CONSTRAINT fk_smp_process_documentidentifierscheme;
ALTER TABLE smp_endpoint              DROP CONSTRAINT fk_smp_endpoint_documentidentifierscheme;

-- Change type from "varchar(50)" to "varchar(135)"
ALTER TABLE smp_service_group        ALTER COLUMN businessIdentifier varchar(135) NOT NULL;
ALTER TABLE smp_service_metadata     ALTER COLUMN businessIdentifier varchar(135) NOT NULL;
ALTER TABLE smp_process              ALTER COLUMN businessIdentifier varchar(135) NOT NULL;
ALTER TABLE smp_endpoint             ALTER COLUMN businessIdentifier varchar(135) NOT NULL;
ALTER TABLE smp_ownership            ALTER COLUMN businessIdentifier varchar(135) NOT NULL;
ALTER TABLE smp_service_metadata_red ALTER COLUMN businessIdentifier varchar(135) NOT NULL;

-- Re-add foreign key constraints (ON DELETE CASCADE only, no ON UPDATE CASCADE)
ALTER TABLE smp_ownership             ADD CONSTRAINT fk_smp_ownership_id                        FOREIGN KEY (businessIdentifierScheme, businessIdentifier)                                                                                                 REFERENCES smp_service_group (businessIdentifierScheme, businessIdentifier)                                                                                          ON DELETE CASCADE;
ALTER TABLE smp_service_metadata_red  ADD CONSTRAINT fk_smp_redirect_businessidentifier         FOREIGN KEY (businessIdentifierScheme, businessIdentifier)                                                                                                 REFERENCES smp_service_group (businessIdentifierScheme, businessIdentifier)                                                                                          ON DELETE CASCADE;
ALTER TABLE smp_service_metadata      ADD CONSTRAINT fk_smp_service_metadata_businessidentifier FOREIGN KEY (businessIdentifierScheme, businessIdentifier)                                                                                                 REFERENCES smp_service_group (businessIdentifierScheme, businessIdentifier)                                                                                          ON DELETE CASCADE;
ALTER TABLE smp_process               ADD CONSTRAINT fk_smp_process_documentidentifierscheme    FOREIGN KEY (businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier)                                                   REFERENCES smp_service_metadata (businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier)                                         ON DELETE CASCADE;
ALTER TABLE smp_endpoint              ADD CONSTRAINT fk_smp_endpoint_documentidentifierscheme   FOREIGN KEY (businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier, processIdentifierType, processIdentifier)         REFERENCES smp_process (businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier, processIdentifierType, processIdentifier)        ON DELETE CASCADE;
