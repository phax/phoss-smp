#!/bin/bash

version=5.2.1

docker login --username phelger

# --------------- XML -----------------------

docker push phelger/smp:$version
docker push phelger/smp:latest
docker push phelger/phoss-smp-xml:$version
docker push phelger/phoss-smp-xml:latest

# --------------- SQL -----------------------

docker push phelger/phoss-smp-sql:$version
docker push phelger/phoss-smp-sql:latest

# --------------- MongoDB -----------------------

docker push phelger/phoss-smp-mongodb:$version
docker push phelger/phoss-smp-mongodb:latest

# --------------- finalize

docker logout

