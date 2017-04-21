@echo off
docker build --build-arg VERSION=5.0.1 -t phoss-smp-5.0.1 .
docker build --build-arg VERSION=5.0.2 -t phoss-smp-5.0.2 .
docker tag phoss-smp-5.0.1 phelger/smp:5.0.1
docker tag phoss-smp-5.0.2 phelger/smp:5.0.2
docker tag phoss-smp-5.0.2 phelger/smp:latest
docker login
docker push phelger/smp:5.0.1
docker push phelger/smp:5.0.2
docker push phelger/smp:latest
docker logout
