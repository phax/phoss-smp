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

set version=5.6.2

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
