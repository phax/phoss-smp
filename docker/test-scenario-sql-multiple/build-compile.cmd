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

pushd ..\..
call mvn clean install -DskipTests=true
if errorlevel 1 goto error
popd

rmdir /S /Q smp-binary 
if errorlevel 1 goto error
xcopy /K /R /E /I /S /H /Y ..\..\phoss-smp-webapp-sql\target\phoss-smp-webapp-sql-6.0.0\* smp-binary\
if errorlevel 1 goto error

goto end

:error
pause
:end
