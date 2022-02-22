@REM
@REM Copyright (C) 2015-2022 Philip Helger and contributors
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

set version=5.6.0

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
docker tag phoss-smp-release-binary-mongodb-%version% phelger/phoss-smp-mongodb:latest

