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

-- DROP database IF EXISTS `smp`;
-- alter session set "_ORACLE_SCRIPT"=true;
-- CREATE USER peppol_user IDENTIFIED BY Test1234;
-- GRANT CONNECT TO peppol_user;
-- GRANT CONNECT, RESOURCE to peppol_user;
-- GRANT CREATE SESSION  TO peppol_user;
-- GRANT ALL PRIVILEGES TO peppol_user;
-- alter user peppol_user quota unlimited on users;

-- DROP TABLE smp_user;
CREATE TABLE smp_user (
  username varchar(256) NOT NULL,
  password varchar(256) NOT NULL,
  constraint smp_user_pk PRIMARY KEY (username) using index tablespace USERS
) tablespace USERS;

-- Disable as per v5.5.2
-- INSERT INTO smp_user (username, password) VALUES ('peppol_user','Test1234');

/*change extension clob,*/

-- DROP TABLE smp_service_group;
CREATE TABLE smp_service_group (
  businessIdentifierScheme varchar(25) NOT NULL,
  businessIdentifier       varchar(50) NOT NULL,
  extension                clob,
 constraint smp_service_group_pk PRIMARY KEY (businessIdentifierScheme,businessIdentifier) using index tablespace USERS
) tablespace USERS;

-- DROP TABLE smp_service_metadata;
/*CONSTRAINT FK_smp_service_metadata_businessIdentifier FOREIGN KEY (businessIdentifierScheme, businessIdentifier) REFERENCES smp_service_group (businessIdentifierScheme, businessIdentifier) ON UPDATE CASCADE
extension clob,*/
CREATE TABLE smp_service_metadata (
  businessIdentifierScheme varchar(25)  NOT NULL,
  businessIdentifier       varchar(50)  NOT NULL,
  documentIdentifierScheme varchar(25)  NOT NULL,
  documentIdentifier       varchar(500) NOT NULL,
  extension                clob,
  constraint smp_service_metadata_pk PRIMARY KEY (businessIdentifierScheme,businessIdentifier,documentIdentifierScheme,documentIdentifier)  using index tablespace USERS
) tablespace USERS;
ALTER TABLE smp_service_metadata
ADD CONSTRAINT smp_service_metadata_fk FOREIGN KEY(  businessIdentifierScheme, businessIdentifier )
REFERENCES smp_service_group(  businessIdentifierScheme, businessIdentifier ) ON DELETE CASCADE ENABLE;
CREATE INDEX smp_service_metadata_fk ON smp_service_metadata (businessIdentifierScheme, businessIdentifier  ) TABLESPACE USERS ;
  
/*extension longtext*/
-- DROP TABLE smp_process;
CREATE TABLE smp_process (
  businessIdentifierScheme varchar(25)  NOT NULL,
  businessIdentifier       varchar(50)  NOT NULL,
  documentIdentifierScheme varchar(25)  NOT NULL,
  documentIdentifier       varchar(500) NOT NULL,
  processIdentifierType    varchar(25)  NOT NULL,
  processIdentifier        varchar(200) NOT NULL,
  extension                clob,
  constraint smp_process_pk PRIMARY KEY  (businessIdentifierScheme,businessIdentifier,documentIdentifierScheme,documentIdentifier,processIdentifierType,processIdentifier) using index tablespace USERS
)  tablespace USERS;
ALTER TABLE smp_process
ADD CONSTRAINT smp_process_fk FOREIGN KEY(businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier)
REFERENCES smp_service_metadata(businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier) ON DELETE CASCADE ENABLE;

-- DROP TABLE smp_endpoint;
/*certificate clob NOT NULL,
requireBusinessLevelSignature int(1) NOT NULL,
serviceActivationDate datetime DEFAULT NULL,
serviceDescription longtext NOT NULL,
serviceExpirationDate datetime DEFAULT NULL,
extension longtext,*/
CREATE TABLE smp_endpoint (
  businessIdentifierScheme      varchar(25)  NOT NULL,
  businessIdentifier            varchar(50)  NOT NULL,
  documentIdentifierScheme      varchar(25)  NOT NULL,
  documentIdentifier            varchar(500) NOT NULL,
  processIdentifierType         varchar(25)  NOT NULL,
  processIdentifier             varchar(200) NOT NULL,
  certificate                   clob         NOT NULL,
  endpointReference             varchar(256) NOT NULL,
  minimumAuthenticationLevel    varchar(256) DEFAULT NULL,
  requireBusinessLevelSignature number(1)    NOT NULL,
  serviceActivationDate         date         DEFAULT NULL,
  serviceDescription            clob         NOT NULL,
  serviceExpirationDate         date         DEFAULT NULL,
  technicalContactUrl           varchar(256) NOT NULL,
  technicalInformationUrl       varchar(256) DEFAULT NULL,
  transportProfile              varchar(256) NOT NULL,
  extension                     clob,
  constraint smp_endpoint_pk PRIMARY KEY (businessIdentifierScheme,businessIdentifier,documentIdentifierScheme,documentIdentifier,processIdentifierType,processIdentifier,transportProfile) using index tablespace USERS
)  tablespace USERS;
ALTER TABLE smp_endpoint
ADD CONSTRAINT smp_endpoint_fk FOREIGN KEY(businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier, processIdentifierType, processIdentifier)
REFERENCES smp_process(businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier, processIdentifierType, processIdentifier) ON DELETE CASCADE ENABLE;

-- DROP TABLE smp_ownership;
CREATE TABLE smp_ownership (
  businessIdentifierScheme varchar(25) NOT NULL,
  businessIdentifier       varchar(50) NOT NULL,
  username                 varchar(256) NOT NULL,
  constraint smp_ownership_pk PRIMARY KEY (businessIdentifierScheme,businessIdentifier,username)  using index tablespace USERS
)  tablespace USERS;
ALTER TABLE smp_ownership ADD CONSTRAINT smp_ownership_id_fk FOREIGN KEY(businessIdentifierScheme, businessIdentifier)
  REFERENCES smp_service_group(businessIdentifierScheme, businessIdentifier) ON DELETE CASCADE ENABLE;
CREATE INDEX smp_ownership_id_fk ON smp_ownership (businessIdentifierScheme, businessIdentifier) TABLESPACE USERS;
  
  
--ALTER TABLE smp_ownership ADD CONSTRAINT smp_ownership_username_fk FOREIGN KEY(username)
--REFERENCES smp_user(username) ON DELETE CASCADE ENABLE;
--CREATE INDEX smp_ownership_username_fk ON smp_ownership
--  (username) TABLESPACE USERS ;

/*  extension longtext,
  certificate longtext,*/
-- name "smp_service_metadata_redirection" is too long for Oracle
-- DROP TABLE smp_service_metadata_red;
CREATE TABLE smp_service_metadata_red (
  businessIdentifierScheme varchar(25)  NOT NULL,
  businessIdentifier       varchar(50)  NOT NULL,
  documentIdentifierScheme varchar(25)  NOT NULL,
  documentIdentifier       varchar(500) NOT NULL,
  certificateUID           varchar(256) NULL,
  redirectionUrl           varchar(256) NOT NULL,
  extension                clob,
  certificate              clob,
  constraint smp_service_metadata_red_pk PRIMARY KEY (documentIdentifierScheme,businessIdentifier,businessIdentifierScheme,documentIdentifier)  using index tablespace USERS
)  tablespace USERS;
ALTER TABLE smp_service_metadata_red ADD CONSTRAINT smp_service_metadata_red_fk FOREIGN KEY(businessIdentifierScheme,businessIdentifier)
REFERENCES smp_service_group(businessIdentifierScheme, businessIdentifier) ON DELETE CASCADE ENABLE;
CREATE INDEX smp_service_metadata_red_fk ON smp_service_metadata_red (businessIdentifierScheme, businessIdentifier) TABLESPACE USERS ;

  
-- DROP TABLE smp_bce;
CREATE TABLE smp_bce (
  id          varchar(45)  NOT NULL,
  pid         varchar(255) NOT NULL,
  name        clob         NOT NULL,
  country     varchar(3)   NOT NULL,
  geoinfo     clob,
  identifiers clob,
  websites    clob,
  contacts    clob,
  addon       clob,
  regdate     date DEFAULT NULL,
  constraint smp_bce_pk PRIMARY KEY (id)  using index tablespace USERS
)  tablespace USERS;
