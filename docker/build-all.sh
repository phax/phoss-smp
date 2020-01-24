#!/bin/bash

version=5.2.1

# --------------- Building -----------------------

docker pull tomcat:9-jre11

# --------------- XML -----------------------

docker build --build-arg SMP_VERSION=$version -t phoss-smp-release-binary-xml-$version -f Dockerfile-release-binary-xml .

# legacy names

docker tag phoss-smp-release-binary-xml-$version phelger/smp:$version
docker tag phoss-smp-release-binary-xml-$version phelger/smp:latest

# new names

docker tag phoss-smp-release-binary-xml-$version phelger/phoss-smp-xml:$version
docker tag phoss-smp-release-binary-xml-$version phelger/phoss-smp-xml:latest

# --------------- SQL -----------------------

docker build --build-arg SMP_VERSION=$version -t phoss-smp-release-binary-sql-$version -f Dockerfile-release-binary-sql .
docker tag phoss-smp-release-binary-sql-$version phelger/phoss-smp-sql:$version
docker tag phoss-smp-release-binary-sql-$version phelger/phoss-smp-sql:latest

# --------------- MongoDB -----------------------

docker build --build-arg SMP_VERSION=$version -t phoss-smp-release-binary-mongodb-$version -f Dockerfile-release-binary-mongodb .
docker tag phoss-smp-release-binary-mongodb-$version phelger/phoss-smp-mongodb:$version
docker tag phoss-smp-release-binary-mongodb-$version phelger/phoss-smp-mongodb:latest

