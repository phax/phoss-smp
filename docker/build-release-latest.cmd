@REM
@REM Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
set XVER=5.0.6
docker build --build-arg VERSION=%XVER% -t phoss-smp-release-binary-xml-%XVER% -f Dockerfile-release-binary-xml .
if errorlevel 1 goto end
docker tag phoss-smp-release-binary-xml-%XVER% phelger/smp:%XVER%
docker tag phoss-smp-release-binary-xml-%XVER% phelger/smp:latest
docker login
if errorlevel 1 goto end
docker push phelger/smp:%XVER%
docker push phelger/smp:latest
docker logout
:end
