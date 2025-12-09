# Download and run


If you want to test Oracle via Docker, you need to get a Docker account and download the image from the Oracle Docker registry
Details: https://container-registry.oracle.com/ords/f?p=113:4:1247167206324:::4:P4_REPOSITORY,AI_REPOSITORY,AI_REPOSITORY_NAME,P4_REPOSITORY_NAME,P4_EULA_ID,P4_BUSINESS_AREA_ID:9,9,Oracle%20Database%20Enterprise%20Edition,Oracle%20Database%20Enterprise%20Edition,1,0&cs=3jv0AAJwJsII3284oUbeuYA486dNVFUeHiJB5rYDCCqTpKEqim_v_mJHtTFsZIM_7eXePZ-A5sV-h0WBjo7FoAA

Run it like this, with a persistent volume:
```
docker run -d --name orcl19c -p 1521:1521 -p 5500:5500 -e ORACLE_PWD=password -v OracleDBData:/opt/oracle/oradata container-registry.oracle.com/database/enterprise:19.19.0.0
```

# Init DB

```
-- Run this as SYS or SYSTEM in the PDB (e.g. ORCLPDB1)
-- Connect with: sqlplus sys/password@localhost:1521/ORCLPDB1 as sysdba

-- Unquote names get uppercases anyway
CREATE USER SMP IDENTIFIED BY smp;

ALTER SESSION SET CURRENT_SCHEMA = SMP;

GRANT CREATE SESSION TO SMP;
GRANT CREATE TABLE TO SMP;
GRANT CREATE SEQUENCE TO SMP;
GRANT CREATE TRIGGER TO SMP;
GRANT CREATE VIEW TO SMP;
GRANT CREATE PROCEDURE TO SMP;
GRANT CREATE TYPE TO SMP;
GRANT CREATE MATERIALIZED VIEW TO SMP;

-- Optional: Unlimited quota on default tablespace (common for dev)
GRANT UNLIMITED TABLESPACE TO SMP;
```
