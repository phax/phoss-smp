CREATE TABLE smp_pmigration (
  id varchar(45) NOT NULL,
  direction varchar(45) NOT NULL,
  state varchar(45) NOT NULL,
  pid varchar(255) NOT NULL,
  initdt timestamp NOT NULL,
  migkey varchar(45) NOT NULL,
  PRIMARY KEY (id)
);
