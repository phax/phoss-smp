--
-- Copyright (C) 2015-2016 Philip Helger (www.helger.com)
-- philip[at]helger[dot]com
--
-- Version: MPL 1.1/EUPL 1.1
--
-- The contents of this file are subject to the Mozilla Public License Version
-- 1.1 (the "License"); you may not use this file except in compliance with
-- the License. You may obtain a copy of the License at:
-- http://www.mozilla.org/MPL/
--
-- Software distributed under the License is distributed on an "AS IS" basis,
-- WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
-- for the specific language governing rights and limitations under the
-- License.
--
-- The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
--
-- Alternatively, the contents of this file may be used under the
-- terms of the EUPL, Version 1.1 or - as soon they will be approved
-- by the European Commission - subsequent versions of the EUPL
-- (the "Licence"); You may not use this work except in compliance
-- with the Licence.
-- You may obtain a copy of the Licence at:
-- http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the Licence is distributed on an "AS IS" basis,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the Licence for the specific language governing permissions and
-- limitations under the Licence.
--
-- If you wish to allow use of your version of this file only
-- under the terms of the EUPL License and not to allow others to use
-- your version of this file under the MPL, indicate your decision by
-- deleting the provisions above and replace them with the notice and
-- other provisions required by the EUPL License. If you do not delete
-- the provisions above, a recipient may use your version of this file
-- under either the MPL or the EUPL License.
--

USE `smp`;

-- internal script to reset the complete DB
-- Compare to the DB backup, this is based on the EclipseLink generated code
ALTER TABLE smp_endpoint DROP FOREIGN KEY FK_smp_endpoint_documentIdentifier;
ALTER TABLE smp_ownership DROP FOREIGN KEY FK_smp_ownership_username;
ALTER TABLE smp_ownership DROP FOREIGN KEY FK_smp_ownership_businessIdentifierScheme;
ALTER TABLE smp_process DROP FOREIGN KEY FK_smp_process_businessIdentifierScheme;
ALTER TABLE smp_service_metadata DROP FOREIGN KEY FK_smp_service_metadata_businessIdentifierScheme;
DROP TABLE smp_bce;
DROP TABLE smp_endpoint;
DROP TABLE smp_ownership;
DROP TABLE smp_process;
DROP TABLE smp_service_group;
DROP TABLE smp_service_metadata;
DROP TABLE smp_service_metadata_redirection;
DROP TABLE smp_user;
CREATE TABLE smp_bce (id VARCHAR(255) NOT NULL, addon VARCHAR(255), contacts VARCHAR(255), country VARCHAR(3) NOT NULL, geoinfo VARCHAR(255), identifiers VARCHAR(255), name VARCHAR(255) NOT NULL, pid VARCHAR(255) NOT NULL, regdate DATE, websites VARCHAR(255), PRIMARY KEY (id));
CREATE TABLE smp_endpoint (certificate LONGTEXT NOT NULL, endpointReference VARCHAR(256) NOT NULL, extension LONGTEXT, minimumAuthenticationLevel VARCHAR(256), requireBusinessLevelSignature TINYINT(1) default 0 NOT NULL, serviceActivationDate DATETIME, serviceDescription LONGTEXT NOT NULL, serviceExpirationDate DATETIME, technicalContactUrl VARCHAR(256) NOT NULL, technicalInformationUrl VARCHAR(256), transportProfile VARCHAR(256) NOT NULL, documentIdentifier VARCHAR(500) NOT NULL, businessIdentifierScheme VARCHAR(25) NOT NULL, processIdentifierType VARCHAR(25) NOT NULL, businessIdentifier VARCHAR(50) NOT NULL, processIdentifier VARCHAR(200) NOT NULL, documentIdentifierScheme VARCHAR(25) NOT NULL, PRIMARY KEY (transportProfile, documentIdentifier, businessIdentifierScheme, processIdentifierType, businessIdentifier, processIdentifier, documentIdentifierScheme));
CREATE TABLE smp_ownership (businessIdentifierScheme VARCHAR(25) NOT NULL, businessIdentifier VARCHAR(50) NOT NULL, username VARCHAR(256) NOT NULL, PRIMARY KEY (businessIdentifierScheme, businessIdentifier, username));
CREATE TABLE smp_process (extension LONGTEXT, documentIdentifier VARCHAR(500) NOT NULL, businessIdentifierScheme VARCHAR(25) NOT NULL, processIdentifierType VARCHAR(25) NOT NULL, businessIdentifier VARCHAR(50) NOT NULL, processIdentifier VARCHAR(200) NOT NULL, documentIdentifierScheme VARCHAR(25) NOT NULL, PRIMARY KEY (documentIdentifier, businessIdentifierScheme, processIdentifierType, businessIdentifier, processIdentifier, documentIdentifierScheme));
CREATE TABLE smp_service_group (extension LONGTEXT, businessIdentifierScheme VARCHAR(25) NOT NULL, businessIdentifier VARCHAR(50) NOT NULL, PRIMARY KEY (businessIdentifierScheme, businessIdentifier));
CREATE TABLE smp_service_metadata (extension LONGTEXT, businessIdentifierScheme VARCHAR(25) NOT NULL, documentIdentifierScheme VARCHAR(25) NOT NULL, businessIdentifier VARCHAR(50) NOT NULL, documentIdentifier VARCHAR(500) NOT NULL, PRIMARY KEY (businessIdentifierScheme, documentIdentifierScheme, businessIdentifier, documentIdentifier));
CREATE TABLE smp_service_metadata_redirection (certificateUID VARCHAR(256) NOT NULL, extension LONGTEXT, redirectionUrl VARCHAR(256) NOT NULL, businessIdentifierScheme VARCHAR(25) NOT NULL, documentIdentifierScheme VARCHAR(25) NOT NULL, businessIdentifier VARCHAR(50) NOT NULL, documentIdentifier VARCHAR(500) NOT NULL, PRIMARY KEY (businessIdentifierScheme, documentIdentifierScheme, businessIdentifier, documentIdentifier));
CREATE TABLE smp_user (username VARCHAR(256) NOT NULL, password VARCHAR(256) NOT NULL, PRIMARY KEY (username));
ALTER TABLE smp_endpoint ADD CONSTRAINT FK_smp_endpoint_documentIdentifier FOREIGN KEY (documentIdentifier, businessIdentifierScheme, processIdentifierType, businessIdentifier, processIdentifier, documentIdentifierScheme) REFERENCES smp_process (documentIdentifier, businessIdentifierScheme, processIdentifierType, businessIdentifier, processIdentifier, documentIdentifierScheme);
ALTER TABLE smp_ownership ADD CONSTRAINT FK_smp_ownership_username FOREIGN KEY (username) REFERENCES smp_user (username);
ALTER TABLE smp_ownership ADD CONSTRAINT FK_smp_ownership_businessIdentifierScheme FOREIGN KEY (businessIdentifierScheme, businessIdentifier) REFERENCES smp_service_group (businessIdentifierScheme, businessIdentifier);
ALTER TABLE smp_process ADD CONSTRAINT FK_smp_process_businessIdentifierScheme FOREIGN KEY (businessIdentifierScheme, documentIdentifierScheme, businessIdentifier, documentIdentifier) REFERENCES smp_service_metadata (businessIdentifierScheme, documentIdentifierScheme, businessIdentifier, documentIdentifier);
ALTER TABLE smp_service_metadata ADD CONSTRAINT FK_smp_service_metadata_businessIdentifierScheme FOREIGN KEY (businessIdentifierScheme, businessIdentifier) REFERENCES smp_service_group (businessIdentifierScheme, businessIdentifier);
INSERT INTO `smp_user` VALUES ('peppol_user','Test1234');
