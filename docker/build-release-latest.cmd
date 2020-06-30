@REM
@REM Copyright (C) 2015-2020 Philip Helger and contributors
@REM philip[at]helger[dot]com
@REM
@REM Licensed under the Apache License, Version 2.0 (the "License");
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM
@REM         http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM

@echo off
set XVER=5.2.4

@REM --------------- Building -----------------------

docker pull tomcat:9-jre11
if errorlevel 1 goto end

@REM --------------- XML -----------------------

docker build --build-arg SMP_VERSION=%XVER% -t phoss-smp-release-binary-xml-%XVER% -f Dockerfile-release-binary-xml .
if errorlevel 1 goto end

@REM legacy names

docker tag phoss-smp-release-binary-xml-%XVER% phelger/smp:%XVER%
if errorlevel 1 goto end

docker tag phoss-smp-release-binary-xml-%XVER% phelger/smp:latest
if errorlevel 1 goto end

@REM new names

docker tag phoss-smp-release-binary-xml-%XVER% phelger/phoss-smp-xml:%XVER%
if errorlevel 1 goto end

docker tag phoss-smp-release-binary-xml-%XVER% phelger/phoss-smp-xml:latest
if errorlevel 1 goto end

@REM --------------- SQL -----------------------

docker build --build-arg SMP_VERSION=%XVER% -t phoss-smp-release-binary-sql-%XVER% -f Dockerfile-release-binary-sql .
if errorlevel 1 goto end

docker tag phoss-smp-release-binary-sql-%XVER% phelger/phoss-smp-sql:%XVER%
if errorlevel 1 goto end

docker tag phoss-smp-release-binary-sql-%XVER% phelger/phoss-smp-sql:latest
if errorlevel 1 goto end

@REM --------------- MongoDB -----------------------

docker build --build-arg SMP_VERSION=%XVER% -t phoss-smp-release-binary-mongodb-%XVER% -f Dockerfile-release-binary-mongodb .
if errorlevel 1 goto end

docker tag phoss-smp-release-binary-mongodb-%XVER% phelger/phoss-smp-mongodb:%XVER%
if errorlevel 1 goto end

docker tag phoss-smp-release-binary-mongodb-%XVER% phelger/phoss-smp-mongodb:latest
if errorlevel 1 goto end

@REM --------------- Pushing -----------------------

docker login
if errorlevel 1 goto end

@REM --------------- XML -----------------------

docker push phelger/smp:%XVER%
if errorlevel 1 goto end

docker push phelger/smp:latest
if errorlevel 1 goto end

docker push phelger/phoss-smp-xml:%XVER%
if errorlevel 1 goto end

docker push phelger/phoss-smp-xml:latest
if errorlevel 1 goto end

@REM --------------- SQL -----------------------

docker push phelger/phoss-smp-sql:%XVER%
if errorlevel 1 goto end

docker push phelger/phoss-smp-sql:latest
if errorlevel 1 goto end

@REM --------------- MongoDB -----------------------

docker push phelger/phoss-smp-mongodb:%XVER%
if errorlevel 1 goto end

docker push phelger/phoss-smp-mongodb:latest
if errorlevel 1 goto end

docker logout
if errorlevel 1 goto end

:end
