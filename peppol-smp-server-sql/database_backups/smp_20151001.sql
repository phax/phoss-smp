--
-- Copyright (C) 2015 Philip Helger (www.helger.com)
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

CREATE DATABASE  IF NOT EXISTS `smp` /*!40100 DEFAULT CHARACTER SET utf8 */;
USE `smp`;
-- MySQL dump 10.13  Distrib 5.1.40, for Win32 (ia32)
--
-- Host: localhost    Database: smp
-- ------------------------------------------------------
-- Server version 5.0.51a-community-nt

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Not dumping tablespaces as no INFORMATION_SCHEMA.FILES table on this server
--

--
-- Table structure for table `smp_process`
--

DROP TABLE IF EXISTS `smp_process`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smp_process` (
  `processIdentifier` varchar(256) NOT NULL,
  `processIdentifierType` varchar(256) NOT NULL,
  `extension` TEXT NULL DEFAULT NULL,
  `businessIdentifier` varchar(256) NOT NULL,
  `businessIdentifierScheme` varchar(256) NOT NULL,
  `documentIdentifier` varchar(256) NOT NULL,
  `documentIdentifierScheme` varchar(256) NOT NULL,
  PRIMARY KEY  (`processIdentifier`,`processIdentifierType`,`businessIdentifier`,`businessIdentifierScheme`,`documentIdentifier`,`documentIdentifierScheme`),
  KEY `FK_process_1` (`documentIdentifier`,`documentIdentifierScheme`,`businessIdentifier`,`businessIdentifierScheme`),
  CONSTRAINT `FK_process_1` FOREIGN KEY (`documentIdentifier`, `documentIdentifierScheme`, `businessIdentifier`, `businessIdentifierScheme`) REFERENCES `smp_service_metadata` (`documentIdentifier`, `documentIdentifierScheme`, `businessIdentifier`, `businessIdentifierScheme`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smp_process`
--

LOCK TABLES `smp_process` WRITE;
/*!40000 ALTER TABLE `smp_process` DISABLE KEYS */;
/*!40000 ALTER TABLE `smp_process` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `smp_user`
--

DROP TABLE IF EXISTS `smp_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smp_user` (
  `username` varchar(256) NOT NULL,
  `password` varchar(256) NOT NULL,
  PRIMARY KEY  (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smp_user`
--

LOCK TABLES `smp_user` WRITE;
/*!40000 ALTER TABLE `smp_user` DISABLE KEYS */;
INSERT INTO `smp_user` VALUES ('peppol_user','Test1234');
/*!40000 ALTER TABLE `smp_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `smp_service_metadata_redirection`
--

DROP TABLE IF EXISTS `smp_service_metadata_redirection`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smp_service_metadata_redirection` (
  `businessIdentifier` varchar(256) NOT NULL,
  `businessIdentifierScheme` varchar(256) NOT NULL,
  `documentIdentifier` varchar(256) NOT NULL,
  `documentIdentifierScheme` varchar(256) NOT NULL,
  `redirectionUrl` varchar(256) NOT NULL,
  `extension` TEXT NULL DEFAULT NULL,
  `certificateUID` varchar(256) NOT NULL,
  PRIMARY KEY  (`businessIdentifier`,`businessIdentifierScheme`,`documentIdentifier`,`documentIdentifierScheme`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smp_service_metadata_redirection`
--

LOCK TABLES `smp_service_metadata_redirection` WRITE;
/*!40000 ALTER TABLE `smp_service_metadata_redirection` DISABLE KEYS */;
/*!40000 ALTER TABLE `smp_service_metadata_redirection` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `smp_endpoint`
--

DROP TABLE IF EXISTS `smp_endpoint`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smp_endpoint` (
  `transportProfile` varchar(256) NOT NULL,
  `endpointReference` varchar(256) NOT NULL,
  `extension` TEXT NULL DEFAULT NULL,
  `processIdentifier` varchar(256) NOT NULL,
  `processIdentifierType` varchar(256) NOT NULL,
  `businessIdentifier` varchar(256) NOT NULL,
  `businessIdentifierScheme` varchar(256) NOT NULL,
  `documentIdentifier` varchar(256) NOT NULL,
  `documentIdentifierScheme` varchar(256) NOT NULL default '',
  `requireBusinessLevelSignature` bit(1) NOT NULL,
  `minimumAuthenticationLevel` varchar(256) default NULL,
  `serviceActivationDate` datetime default NULL,
  `serviceExpirationDate` datetime default NULL,
  `certificate` TEXT NOT NULL,
  `serviceDescription` TEXT NOT NULL,
  `technicalContactUrl` varchar(256) NOT NULL,
  `technicalInformationUrl` varchar(256) default NULL,
  PRIMARY KEY  (`businessIdentifierScheme`,`businessIdentifier`,`processIdentifierType`,`processIdentifier`,`documentIdentifier`,`documentIdentifierScheme`,`endpointReference`,`transportProfile`),
  KEY `FK_endpoint_1` (`processIdentifier`,`processIdentifierType`,`businessIdentifier`,`businessIdentifierScheme`,`documentIdentifier`,`documentIdentifierScheme`),
  CONSTRAINT `FK_endpoint_1` FOREIGN KEY (`processIdentifier`, `processIdentifierType`, `businessIdentifier`, `businessIdentifierScheme`, `documentIdentifier`, `documentIdentifierScheme`) REFERENCES `smp_process` (`processIdentifier`, `processIdentifierType`, `businessIdentifier`, `businessIdentifierScheme`, `documentIdentifier`, `documentIdentifierScheme`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smp_endpoint`
--

LOCK TABLES `smp_endpoint` WRITE;
/*!40000 ALTER TABLE `smp_endpoint` DISABLE KEYS */;
/*!40000 ALTER TABLE `smp_endpoint` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `smp_ownership`
--

DROP TABLE IF EXISTS `smp_ownership`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smp_ownership` (
  `username` varchar(256) NOT NULL,
  `businessIdentifier` varchar(256) NOT NULL,
  `businessIdentifierScheme` varchar(256) NOT NULL,
  KEY `FK_ownership_1` (`businessIdentifier`,`businessIdentifierScheme`),
  KEY `FK_ownership_2` (`username`),
  CONSTRAINT `FK_ownership_2` FOREIGN KEY (`username`) REFERENCES `smp_user` (`username`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `FK_ownership_1` FOREIGN KEY (`businessIdentifier`, `businessIdentifierScheme`) REFERENCES `smp_service_group` (`businessIdentifier`, `businessIdentifierScheme`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smp_ownership`
--

LOCK TABLES `smp_ownership` WRITE;
/*!40000 ALTER TABLE `smp_ownership` DISABLE KEYS */;
/*!40000 ALTER TABLE `smp_ownership` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `smp_service_metadata`
--

DROP TABLE IF EXISTS `smp_service_metadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smp_service_metadata` (
  `documentIdentifier` varchar(256) NOT NULL,
  `documentIdentifierScheme` varchar(256) NOT NULL,
  `businessIdentifier` varchar(256) NOT NULL,
  `businessIdentifierScheme` varchar(256) NOT NULL,
  `extension` TEXT NULL DEFAULT NULL,
  PRIMARY KEY  (`documentIdentifier`,`documentIdentifierScheme`,`businessIdentifier`,`businessIdentifierScheme`),
  KEY `FK_service_metadata_1` (`businessIdentifier`,`businessIdentifierScheme`),
  CONSTRAINT `FK_service_metadata_1` FOREIGN KEY (`businessIdentifier`, `businessIdentifierScheme`) REFERENCES `smp_service_group` (`businessIdentifier`, `businessIdentifierScheme`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smp_service_metadata`
--

LOCK TABLES `smp_service_metadata` WRITE;
/*!40000 ALTER TABLE `smp_service_metadata` DISABLE KEYS */;
/*!40000 ALTER TABLE `smp_service_metadata` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `smp_service_group`
--

DROP TABLE IF EXISTS `smp_service_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smp_service_group` (
  `businessIdentifier` varchar(256) NOT NULL,
  `businessIdentifierScheme` varchar(256) NOT NULL,
  `extension` TEXT NULL DEFAULT NULL,
  PRIMARY KEY  (`businessIdentifier`,`businessIdentifierScheme`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smp_service_group`
--

LOCK TABLES `smp_service_group` WRITE;
/*!40000 ALTER TABLE `smp_service_group` DISABLE KEYS */;
/*!40000 ALTER TABLE `smp_service_group` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2011-02-22 19:41:35
