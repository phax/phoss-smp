CREATE TABLE `smp_pmigration` (
  `id` varchar(45) NOT NULL COMMENT 'Internal ID',
  `direction` varchar(45) NOT NULL COMMENT 'Migration direction',
  `state` varchar(45) NOT NULL COMMENT 'Migration state',
  `pid` varchar(255) NOT NULL COMMENT 'Participant/Business ID',
  `initdt` datetime NOT NULL COMMENT 'The date and time when the migration was initiated',
  `migkey` varchar(45) NOT NULL COMMENT 'The migration key itself',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COMMENT='SMP Participant Migration Entity';
