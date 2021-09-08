CREATE TABLE smp_pmigration (
  id varchar(45) NOT NULL,
  direction varchar(45) NOT NULL,
  state varchar(45) NOT NULL,
  pid varchar(255) NOT NULL,
  initdt timestamp NOT NULL,
  migkey varchar(45) NOT NULL,
  constraint smp_pmigration_pk PRIMARY KEY  (id)  using index tablespace USERS
) tablespace USERS;


COMMENT ON COLUMN smp_pmigration.id IS  'Internal ID';
COMMENT ON COLUMN smp_pmigration.direction IS  'Migration direction';
COMMENT ON COLUMN smp_pmigration.state IS  'Migration state';
COMMENT ON COLUMN smp_pmigration.pid IS  'Participant/Business ID';
COMMENT ON COLUMN smp_pmigration.initdt IS  'The date and time when the migration was initiated';
COMMENT ON COLUMN smp_pmigration.migkey IS  'The migration key itself';   
