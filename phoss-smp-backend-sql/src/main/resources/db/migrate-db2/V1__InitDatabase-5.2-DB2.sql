--
-- Copyright (C) 2019-2024 Philip Helger and contributors
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

--<ScriptOptions statementTerminator=";"/>


CREATE TABLE smp_user (
    username VARCHAR(256) NOT NULL, 
    password VARCHAR(256) NOT NULL, 
    CONSTRAINT pk_smp_user PRIMARY KEY
      (username)
  );


CREATE TABLE smp_service_group (
    businessIdentifierScheme VARCHAR(25) NOT NULL, 
    businessIdentifier       VARCHAR(50) NOT NULL, 
    extension                CLOB, 
    CONSTRAINT pk_smp_service_group PRIMARY KEY
      (businessIdentifierScheme, businessIdentifier)
  );


CREATE TABLE smp_service_metadata (
    businessIdentifierScheme VARCHAR(25)  NOT NULL, 
    businessIdentifier       VARCHAR(50)  NOT NULL, 
    documentIdentifierScheme VARCHAR(25)  NOT NULL, 
    documentIdentifier       VARCHAR(500) NOT NULL, 
    extension                CLOB, 
    CONSTRAINT pk_smp_service_metadata PRIMARY KEY
      (businessIdentifierScheme, businessIdentifier, 
       documentIdentifierScheme, documentIdentifier)
  );


CREATE TABLE smp_process (
    businessIdentifierScheme VARCHAR(25)  NOT NULL, 
    businessIdentifier       VARCHAR(50)  NOT NULL, 
    documentIdentifierScheme VARCHAR(25)  NOT NULL, 
    documentIdentifier       VARCHAR(500) NOT NULL, 
    processIdentifierType    VARCHAR(25)  NOT NULL, 
    processIdentifier        VARCHAR(200) NOT NULL, 
    extension                CLOB, 
    CONSTRAINT pk_smp_process PRIMARY KEY
      (businessIdentifierScheme, businessIdentifier, 
       documentIdentifierScheme, documentIdentifier, 
       processIdentifierType, processIdentifier)
  );


CREATE TABLE smp_endpoint (
    businessIdentifierScheme      VARCHAR(25)  NOT NULL, 
    businessIdentifier            VARCHAR(50)  NOT NULL, 
    documentIdentifierScheme      VARCHAR(25)  NOT NULL, 
    documentIdentifier            VARCHAR(500) NOT NULL, 
    processIdentifierType         VARCHAR(25)  NOT NULL, 
    processIdentifier             VARCHAR(200) NOT NULL, 
    certificate                   CLOB         NOT NULL, 
    endpointReference             VARCHAR(256) NOT NULL, 
    minimumAuthenticationLevel    VARCHAR(256) DEFAULT NULL, 
    requireBusinessLevelSignature SMALLINT     NOT NULL, 
    serviceActivationDate         TIMESTAMP    DEFAULT NULL, 
    serviceDescription            CLOB         NOT NULL, 
    serviceExpirationDate         TIMESTAMP    DEFAULT NULL, 
    technicalContactUrl           VARCHAR(256) NOT NULL, 
    technicalInformationUrl       VARCHAR(256) DEFAULT NULL, 
-- [DB2] avoid maximum PK length is exceeded
    transportProfile              VARCHAR(45)  NOT NULL, 
    extension                     CLOB,
    CONSTRAINT pk_smp_endpoint PRIMARY KEY
      (businessIdentifierScheme, businessIdentifier, 
       documentIdentifierScheme, documentIdentifier, 
       processIdentifierType, processIdentifier, 
       transportProfile)
  );


CREATE TABLE smp_ownership (
    businessIdentifierScheme VARCHAR(25)  NOT NULL, 
    businessIdentifier       VARCHAR(50)  NOT NULL, 
    username                 VARCHAR(256) NOT NULL, 
    CONSTRAINT pk_smp_ownership PRIMARY KEY
      (businessIdentifierScheme, businessIdentifier, 
       username)
  );


CREATE TABLE smp_service_metadata_red (
    businessIdentifierScheme VARCHAR(25) NOT NULL, 
    businessIdentifier       VARCHAR(50) NOT NULL, 
    documentIdentifierScheme VARCHAR(25) NOT NULL, 
    documentIdentifier       VARCHAR(500) NOT NULL, 
    certificateUID           VARCHAR(256), 
    redirectionUrl           VARCHAR(256) NOT NULL, 
    extension                CLOB, 
    certificate              CLOB, 
    CONSTRAINT pk_smp_service_metadata_red PRIMARY KEY
      (businessIdentifierScheme, businessIdentifier,
       documentIdentifierScheme, documentIdentifier)
  );


CREATE TABLE smp_bce (
    id          VARCHAR(45)  NOT NULL, 
    pid         VARCHAR(255) NOT NULL, 
    name        CLOB         NOT NULL, 
    country     VARCHAR(3)   NOT NULL, 
    geoinfo     CLOB, 
    identifiers CLOB, 
    websites    CLOB, 
    contacts    CLOB, 
    addon       CLOB, 
    regdate     DATE, 
    CONSTRAINT pk_smp_bce PRIMARY KEY (id)
  );


CREATE INDEX idx_smp_bce ON smp_bce
   (pid    ASC);

CREATE INDEX idx_smp_process ON smp_process
   (businessIdentifierScheme ASC, businessIdentifier ASC, 
    documentIdentifierScheme ASC, documentIdentifier ASC);

CREATE INDEX idx_smp_service_metadata ON smp_service_metadata
   (businessIdentifierScheme ASC, businessIdentifier ASC);


ALTER TABLE smp_endpoint ADD CONSTRAINT fk_smp_endpoint_documentidentifierscheme FOREIGN KEY
  (businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier, processIdentifierType, processIdentifier)
  REFERENCES smp_process
  (businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier, processIdentifierType, processIdentifier)
  ON DELETE CASCADE;

ALTER TABLE smp_ownership ADD CONSTRAINT fk_smp_ownership_id FOREIGN KEY
  (businessIdentifierScheme, businessIdentifier)
  REFERENCES smp_service_group
  (businessIdentifierScheme, businessIdentifier)
  ON DELETE CASCADE;

-- Is dropped later anyway
--ALTER TABLE smp_ownership ADD CONSTRAINT fk_smp_ownership_username FOREIGN KEY
--  (username)
--  REFERENCES smp_user
--  (username)
--  ON DELETE CASCADE;

ALTER TABLE smp_process ADD CONSTRAINT fk_smp_process_documentidentifierscheme FOREIGN KEY
  (businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier)
  REFERENCES smp_service_metadata
  (businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier)
  ON DELETE CASCADE;

ALTER TABLE smp_service_metadata ADD CONSTRAINT fk_smp_service_metadata_businessidentifier FOREIGN KEY
  (businessIdentifierScheme, businessIdentifier)
  REFERENCES smp_service_group
  (businessIdentifierScheme, businessIdentifier)
  ON DELETE CASCADE;

ALTER TABLE smp_service_metadata_red ADD CONSTRAINT fk_smp_redirect_businessidentifier FOREIGN KEY
  (businessIdentifierScheme, businessIdentifier)
  REFERENCES smp_service_group
  (businessIdentifierScheme, businessIdentifier)
  ON DELETE CASCADE;
