@echo off

set version=5.4.0

rem --------------- XML -----------------------

docker build --pull --build-arg SMP_VERSION=%version% -t phoss-smp-release-binary-xml-%version% -f Dockerfile-release-binary-xml .

rem legacy names

docker tag phoss-smp-release-binary-xml-%version% phelger/smp:%version%
docker tag phoss-smp-release-binary-xml-%version% phelger/smp:latest

rem new names

docker tag phoss-smp-release-binary-xml-%version% phelger/phoss-smp-xml:%version%
docker tag phoss-smp-release-binary-xml-%version% phelger/phoss-smp-xml:latest

rem --------------- SQL -----------------------

docker build --pull --build-arg SMP_VERSION=%version% -t phoss-smp-release-binary-sql-%version% -f Dockerfile-release-binary-sql .
docker tag phoss-smp-release-binary-sql-%version% phelger/phoss-smp-sql:%version%
docker tag phoss-smp-release-binary-sql-%version% phelger/phoss-smp-sql:latest

rem --------------- MongoDB -----------------------

docker build --pull --build-arg SMP_VERSION=%version% -t phoss-smp-release-binary-mongodb-%version% -f Dockerfile-release-binary-mongodb .
docker tag phoss-smp-release-binary-mongodb-%version% phelger/phoss-smp-mongodb:%version%
docker tag phoss-smp-release-binary-mongodb-$version phelger/phoss-smp-mongodb:latest

