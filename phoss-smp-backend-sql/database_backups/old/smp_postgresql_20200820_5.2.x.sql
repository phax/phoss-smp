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

CREATE DATABASE smp WITH ENCODING = 'UTF8' OWNER = smp;

CREATE TABLE smp_user (
  username varchar(256) NOT NULL,
  password varchar(256) NOT NULL,
  PRIMARY KEY (username)
);
INSERT INTO smp_user VALUES ('peppol_user','Test1234');

CREATE TABLE smp_service_group (
  businessIdentifierScheme varchar(25) NOT NULL,
  businessIdentifier varchar(50) NOT NULL,
  extension text,
  PRIMARY KEY (businessIdentifierScheme,businessIdentifier)
);

CREATE TABLE smp_service_metadata (
  businessIdentifierScheme varchar(25) NOT NULL,
  businessIdentifier varchar(50) NOT NULL,
  documentIdentifierScheme varchar(25) NOT NULL,
  documentIdentifier varchar(500) NOT NULL,
  extension text,
  PRIMARY KEY (businessIdentifierScheme,businessIdentifier,documentIdentifierScheme,documentIdentifier),
  CONSTRAINT FK_smp_service_metadata_businessIdentifier FOREIGN KEY (businessIdentifierScheme, businessIdentifier) REFERENCES smp_service_group (businessIdentifierScheme, businessIdentifier) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE smp_process (
  businessIdentifierScheme varchar(25) NOT NULL,
  businessIdentifier varchar(50) NOT NULL,
  documentIdentifierScheme varchar(25) NOT NULL,
  documentIdentifier varchar(500) NOT NULL,
  processIdentifierType varchar(25) NOT NULL,
  processIdentifier varchar(200) NOT NULL,
  extension text,
  PRIMARY KEY (businessIdentifierScheme,businessIdentifier,documentIdentifierScheme,documentIdentifier,processIdentifierType,processIdentifier),
  CONSTRAINT FK_smp_process_documentIdentifierScheme FOREIGN KEY (businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier) REFERENCES smp_service_metadata (businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE smp_endpoint (
  businessIdentifierScheme varchar(25) NOT NULL,
  businessIdentifier varchar(50) NOT NULL,
  documentIdentifierScheme varchar(25) NOT NULL,
  documentIdentifier varchar(500) NOT NULL,
  processIdentifierType varchar(25) NOT NULL,
  processIdentifier varchar(200) NOT NULL,
  certificate text NOT NULL,
  endpointReference varchar(256) NOT NULL,
  minimumAuthenticationLevel varchar(256) DEFAULT NULL,
  requireBusinessLevelSignature boolean NOT NULL,
  serviceActivationDate timestamp DEFAULT NULL,
  serviceDescription text NOT NULL,
  serviceExpirationDate timestamp DEFAULT NULL,
  technicalContactUrl varchar(256) NOT NULL,
  technicalInformationUrl varchar(256) DEFAULT NULL,
  transportProfile varchar(256) NOT NULL,
  extension text,
  PRIMARY KEY (businessIdentifierScheme,businessIdentifier,documentIdentifierScheme,documentIdentifier,processIdentifierType,processIdentifier,transportProfile),
  CONSTRAINT FK_smp_endpoint_documentIdentifierScheme FOREIGN KEY (businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier, processIdentifierType, processIdentifier) REFERENCES smp_process (businessIdentifierScheme, businessIdentifier, documentIdentifierScheme, documentIdentifier, processIdentifierType, processIdentifier) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE smp_ownership (
  businessIdentifierScheme varchar(25) NOT NULL,
  businessIdentifier varchar(50) NOT NULL,
  username varchar(256) NOT NULL,
  PRIMARY KEY (businessIdentifierScheme,businessIdentifier,username),
  CONSTRAINT FK_smp_ownership_id FOREIGN KEY (businessIdentifierScheme, businessIdentifier) REFERENCES smp_service_group (businessIdentifierScheme, businessIdentifier) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT FK_smp_ownership_username FOREIGN KEY (username) REFERENCES smp_user (username) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE smp_service_metadata_redirection (
  businessIdentifierScheme varchar(25) NOT NULL,
  businessIdentifier varchar(50) NOT NULL,
  documentIdentifierScheme varchar(25) NOT NULL,
  documentIdentifier varchar(500) NOT NULL,
  certificateUID varchar(256) DEFAULT NULL,
  redirectionUrl varchar(256) NOT NULL,
  extension text,
  certificate text,
  PRIMARY KEY (documentIdentifierScheme,businessIdentifier,businessIdentifierScheme,documentIdentifier),
  CONSTRAINT FK_smp_redirect_businessIdentifier FOREIGN KEY (businessIdentifierScheme, businessIdentifier) REFERENCES smp_service_group (businessIdentifierScheme, businessIdentifier) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE smp_bce (
  id varchar(45) NOT NULL,
  pid varchar(255) NOT NULL,
  name text NOT NULL,
  country varchar(3) NOT NULL,
  geoinfo text,
  identifiers text,
  websites text,
  contacts text,
  addon text,
  regdate date DEFAULT NULL,
  PRIMARY KEY (id)
);

ALTER TABLE smp_user OWNER to smp;
ALTER TABLE smp_service_group OWNER to smp;
ALTER TABLE smp_service_metadata OWNER to smp;
ALTER TABLE smp_process OWNER to smp;
ALTER TABLE smp_endpoint OWNER to smp;
ALTER TABLE smp_ownership OWNER to smp;
ALTER TABLE smp_service_metadata_redirection OWNER to smp;
ALTER TABLE smp_bce OWNER to smp;
