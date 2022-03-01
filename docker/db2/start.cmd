@echo off
docker run -itd --pull=always --rm --name mydb2 --privileged=true -p 50000:50000 -e LICENSE=accept -e DB2INST1_PASSWORD=password -e DBNAME=testdb -v db2:/database ibmcom/db2:latest

