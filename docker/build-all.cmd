@REM
@REM Copyright (C) 2015-2024 Philip Helger and contributors
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

set version=7.1.2

echo Docker login
docker login --username phelger
if errorlevel 1 goto end

echo Starting buildx
docker buildx create --name phoss-smp node-amd64
if errorlevel 1 goto end

rem --------------- XML -----------------------
docker buildx build --platform=linux/amd64 --push --pull --build-arg SMP_VERSION=%version% -t phelger/smp:%version% -t phelger/smp:latest -t phelger/phoss-smp-xml:%version% -t phelger/phoss-smp-xml:latest -f Dockerfile-release-binary-xml .
docker buildx build --platform=linux/arm64 --push --pull --build-arg SMP_VERSION=%version% -t phelger/phoss-smp-xml-arm64:%version% -t phelger/phoss-smp-xml-arm64:latest -f Dockerfile-release-binary-xml .

rem --------------- SQL -----------------------

docker buildx build --platform=linux/amd64 --push --pull --build-arg SMP_VERSION=%version% -t phelger/phoss-smp-sql:%version% -t phelger/phoss-smp-sql:latest -f Dockerfile-release-binary-sql .
docker buildx build --platform=linux/arm64 --push --pull --build-arg SMP_VERSION=%version% -t phelger/phoss-smp-sql-arm64:%version% -t phelger/phoss-smp-sql-arm64:latest -f Dockerfile-release-binary-sql .

rem --------------- MongoDB -----------------------

docker buildx build --platform=linux/amd64 --push --pull --build-arg SMP_VERSION=%version% -t phelger/phoss-smp-mongodb:%version% -t phelger/phoss-smp-mongodb:latest -f Dockerfile-release-binary-mongodb .
docker buildx build --platform=linux/arm64 --push --pull --build-arg SMP_VERSION=%version% -t phelger/phoss-smp-mongodb-arm64:%version% -t phelger/phoss-smp-mongodb-arm64:latest -f Dockerfile-release-binary-mongodb .

:end
