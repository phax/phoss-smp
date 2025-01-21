@REM
@REM Copyright (C) 2015-2025 Philip Helger and contributors
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
:: default user: admin@helger.com
:: default password: password
curl -X DELETE -H "Content-Type: application/xml" -H "Authorization: Basic YWRtaW5AaGVsZ2VyLmNvbTpwYXNzd29yZA==" -d "iso6523-actorid-upis::9999:test1" -o response-data -i http://localhost:90/iso6523-actorid-upis::9999:test1
