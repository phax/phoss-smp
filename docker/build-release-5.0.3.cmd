@echo off
docker build --build-arg VERSION=5.0.3 -t phoss-smp-5.0.3 .
docker tag phoss-smp-5.0.3 phelger/smp:5.0.3
docker tag phoss-smp-5.0.3 phelger/smp:latest
docker login
docker push phelger/smp:5.0.3
docker push phelger/smp:latest
docker logout
