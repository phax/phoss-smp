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

CREATE TABLE `smp_user` (
  `username` varchar(256) NOT NULL,
  `password` varchar(256) NOT NULL,
  PRIMARY KEY (`username`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Disable as per v5.5.2
-- INSERT INTO `smp_user` VALUES ('peppol_user','Test1234');

CREATE TABLE `smp_service_group` (
  `businessIdentifierScheme` varchar(25) NOT NULL,
  `businessIdentifier`       varchar(50) NOT NULL,
  `extension`                longtext,
  PRIMARY KEY (`businessIdentifierScheme`,`businessIdentifier`),
  UNIQUE KEY `bid` (`businessIdentifierScheme`,`businessIdentifier`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `smp_service_metadata` (
  `businessIdentifierScheme` varchar(25)  NOT NULL,
  `businessIdentifier`       varchar(50)  NOT NULL,
  `documentIdentifierScheme` varchar(25)  NOT NULL,
  `documentIdentifier`       varchar(500) NOT NULL,
  `extension`                longtext,
  PRIMARY KEY (`businessIdentifierScheme`,`businessIdentifier`,`documentIdentifierScheme`,`documentIdentifier`),
  KEY `FK_smp_service_metadata_id` (`businessIdentifierScheme`,`businessIdentifier`),
  CONSTRAINT `FK_smp_service_metadata_businessIdentifier` FOREIGN KEY (`businessIdentifierScheme`, `businessIdentifier`) REFERENCES `smp_service_group` (`businessIdentifierScheme`, `businessIdentifier`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `smp_process` (
  `businessIdentifierScheme` varchar(25)  NOT NULL,
  `businessIdentifier`       varchar(50)  NOT NULL,
  `documentIdentifierScheme` varchar(25)  NOT NULL,
  `documentIdentifier`       varchar(500) NOT NULL,
  `processIdentifierType`    varchar(25)  NOT NULL,
  `processIdentifier`        varchar(200) NOT NULL,
  `extension`                longtext,
  PRIMARY KEY (`businessIdentifierScheme`,`businessIdentifier`,`documentIdentifierScheme`,`documentIdentifier`,`processIdentifierType`,`processIdentifier`),
  KEY `FK_smp_process_id` (`businessIdentifierScheme`,`businessIdentifier`,`documentIdentifierScheme`,`documentIdentifier`),
  CONSTRAINT `FK_smp_process_documentIdentifierScheme` FOREIGN KEY (`businessIdentifierScheme`, `businessIdentifier`, `documentIdentifierScheme`, `documentIdentifier`) REFERENCES `smp_service_metadata` (`businessIdentifierScheme`, `businessIdentifier`, `documentIdentifierScheme`, `documentIdentifier`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- MUST use latin1 otherwise the key in this table is too long
CREATE TABLE `smp_endpoint` (
  `businessIdentifierScheme`      varchar(25)  NOT NULL,
  `businessIdentifier`            varchar(50)  NOT NULL,
  `documentIdentifierScheme`      varchar(25)  NOT NULL,
  `documentIdentifier`            varchar(500) NOT NULL,
  `processIdentifierType`         varchar(25)  NOT NULL,
  `processIdentifier`             varchar(200) NOT NULL,
  `certificate`                   longtext NOT NULL,
  `endpointReference`             varchar(256) NOT NULL,
  `minimumAuthenticationLevel`    varchar(256) DEFAULT NULL,
  `requireBusinessLevelSignature` tinyint(1)   NOT NULL,
  `serviceActivationDate`         datetime     DEFAULT NULL,
  `serviceDescription`            longtext     NOT NULL,
  `serviceExpirationDate`         datetime     DEFAULT NULL,
  `technicalContactUrl`           varchar(256) NOT NULL,
  `technicalInformationUrl`       varchar(256) DEFAULT NULL,
  `transportProfile`              varchar(256) NOT NULL,
  `extension`                     longtext,
  PRIMARY KEY (`businessIdentifierScheme`,`businessIdentifier`,`documentIdentifierScheme`,`documentIdentifier`,`processIdentifierType`,`processIdentifier`,`transportProfile`),
  KEY `FK_smp_endpoint_id` (`businessIdentifierScheme`,`businessIdentifier`,`documentIdentifierScheme`,`documentIdentifier`,`processIdentifierType`,`processIdentifier`),
  CONSTRAINT `FK_smp_endpoint_documentIdentifierScheme` FOREIGN KEY (`businessIdentifierScheme`, `businessIdentifier`, `documentIdentifierScheme`, `documentIdentifier`, `processIdentifierType`, `processIdentifier`) REFERENCES `smp_process` (`businessIdentifierScheme`, `businessIdentifier`, `documentIdentifierScheme`, `documentIdentifier`, `processIdentifierType`, `processIdentifier`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `smp_ownership` (
  `businessIdentifierScheme` varchar(25)  NOT NULL,
  `businessIdentifier`       varchar(50)  NOT NULL,
  `username`                 varchar(256) NOT NULL,
  PRIMARY KEY (`businessIdentifierScheme`,`businessIdentifier`,`username`),
  KEY `FK_smp_ownership_username` (`username`),
  CONSTRAINT `FK_smp_ownership_id` FOREIGN KEY (`businessIdentifierScheme`, `businessIdentifier`) REFERENCES `smp_service_group` (`businessIdentifierScheme`, `businessIdentifier`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `FK_smp_ownership_username` FOREIGN KEY (`username`) REFERENCES `smp_user` (`username`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `smp_service_metadata_redirection` (
  `businessIdentifierScheme` varchar(25)  NOT NULL,
  `businessIdentifier`       varchar(50)  NOT NULL,
  `documentIdentifierScheme` varchar(25)  NOT NULL,
  `documentIdentifier`       varchar(500) NOT NULL,
  `certificateUID`           varchar(256) DEFAULT NULL,
  `redirectionUrl`           varchar(256) NOT NULL,
  `extension`                longtext,
  `certificate`              longtext,
  PRIMARY KEY (`documentIdentifierScheme`,`businessIdentifier`,`businessIdentifierScheme`,`documentIdentifier`),
  KEY `FK_smp_redirect_businessIdentifier` (`businessIdentifierScheme`,`businessIdentifier`),
  CONSTRAINT `FK_smp_redirect_businessIdentifier` FOREIGN KEY (`businessIdentifierScheme`, `businessIdentifier`) REFERENCES `smp_service_group` (`businessIdentifierScheme`, `businessIdentifier`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `smp_bce` (
  `id`          varchar(45)  NOT NULL     COMMENT 'Internal ID',
  `pid`         varchar(255) NOT NULL     COMMENT 'Participant/Business ID',
  `name`        text         NOT NULL     COMMENT 'Entity name',
  `country`     varchar(3)   NOT NULL     COMMENT 'Country code',
  `geoinfo`     text                      COMMENT 'Geographical information',
  `identifiers` text                      COMMENT 'Additional identifiers',
  `websites`    text                      COMMENT 'Website URIs',
  `contacts`    text                      COMMENT 'Contact information',
  `addon`       longtext                  COMMENT 'Additional information',
  `regdate`     date         DEFAULT NULL COMMENT 'Registration date',
  PRIMARY KEY (`id`),
  KEY `FK_pid` (`pid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='SMP Business Card Entity';
