```shell
docker volume create db2_data
```

* DB2 is memory hungy

```shell
docker run -itd --name db2-dev --platform=linux/amd64 --privileged=true --memory=4g -p 50000:50000 -e LICENSE=accept -e DB2INST1_PASSWORD=smp -e DBNAME=smp -v db2_data:/database icr.io/db2_community/db2
```

```shell
docker exec -ti db2-dev bash -c "useradd smp && echo 'smp:smp' | chpasswd"

docker exec -ti db2-dev bash -c "su - db2inst1 -c \"
  db2 connect to smp;
  db2 GRANT DBADM ON DATABASE TO USER smp;
  db2 connect reset;
\""
```
