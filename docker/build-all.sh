#!/bin/bash
#
# Copyright (C) 2015-2022 Philip Helger and contributors
# philip[at]helger[dot]com
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


version=5.7.0

# --------------- XML -----------------------

docker build --pull --build-arg SMP_VERSION=$version -t phoss-smp-release-binary-xml-$version -f Dockerfile-release-binary-xml .

# legacy names

docker tag phoss-smp-release-binary-xml-$version phelger/smp:$version
docker tag phoss-smp-release-binary-xml-$version phelger/smp:latest

# new names

docker tag phoss-smp-release-binary-xml-$version phelger/phoss-smp-xml:$version
docker tag phoss-smp-release-binary-xml-$version phelger/phoss-smp-xml:latest

# --------------- SQL -----------------------

docker build --pull --build-arg SMP_VERSION=$version -t phoss-smp-release-binary-sql-$version -f Dockerfile-release-binary-sql .
docker tag phoss-smp-release-binary-sql-$version phelger/phoss-smp-sql:$version
docker tag phoss-smp-release-binary-sql-$version phelger/phoss-smp-sql:latest

# --------------- MongoDB -----------------------

docker build --pull --build-arg SMP_VERSION=$version -t phoss-smp-release-binary-mongodb-$version -f Dockerfile-release-binary-mongodb .
docker tag phoss-smp-release-binary-mongodb-$version phelger/phoss-smp-mongodb:$version
docker tag phoss-smp-release-binary-mongodb-$version phelger/phoss-smp-mongodb:latest

