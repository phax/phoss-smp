-- Create new table to store Business Cards

CREATE TABLE `smp_bce` (
  `id` varchar(45) NOT NULL COMMENT 'Internal ID',
  `pid` varchar(255) NOT NULL COMMENT 'Participant/Business ID',
  `name` text NOT NULL COMMENT 'Entity name',
  `country` varchar(3) NOT NULL COMMENT 'Country code',
  `geoinfo` text COMMENT 'Geographical information',
  `identifiers` text COMMENT 'Additional identifiers',
  `websites` text COMMENT 'Website URIs',
  `contacts` text COMMENT 'Contact information',
  `addon` longtext COMMENT 'Additional information',
  `regdate` date DEFAULT NULL COMMENT 'Registration date',
  PRIMARY KEY (`id`),
  KEY `FK_pid` (`pid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COMMENT='SMP Business Card Entity';
