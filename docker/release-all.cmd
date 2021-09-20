@echo off

set version=5.4.0

docker login --username phelger

rem --------------- XML -----------------------

docker push phelger/smp:%version%
docker push phelger/smp:latest
docker push phelger/phoss-smp-xml:%version%
docker push phelger/phoss-smp-xml:latest

rem --------------- SQL -----------------------

docker push phelger/phoss-smp-sql:%version%
docker push phelger/phoss-smp-sql:latest

rem --------------- MongoDB -----------------------

docker push phelger/phoss-smp-mongodb:%version%
docker push phelger/phoss-smp-mongodb:latest

rem --------------- finalize

docker logout
