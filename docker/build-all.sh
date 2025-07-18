#!/bin/bash
#
# Copyright (C) 2015-2025 Philip Helger and contributors
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

version=7.2.7

echo Docker login
docker login --username phelger

echo Starting buildx
docker buildx create --name phoss-smp node-amd64

# --------------- XML -----------------------
docker buildx build --platform=linux/amd64 --push --pull --build-arg SMP_VERSION=$version -t phelger/smp:$version -t phelger/smp:latest -t phelger/phoss-smp-xml:$version -t phelger/phoss-smp-xml:latest -f Dockerfile-release-binary-xml .
docker buildx build --platform=linux/arm64 --push --pull --build-arg SMP_VERSION=$version -t phelger/phoss-smp-xml-arm64:$version -t phelger/phoss-smp-xml-arm64:latest -f Dockerfile-release-binary-xml .

# --------------- SQL -----------------------

docker buildx build --platform=linux/amd64 --push --pull --build-arg SMP_VERSION=$version -t phelger/phoss-smp-sql:$version -t phelger/phoss-smp-sql:latest -f Dockerfile-release-binary-sql .
docker buildx build --platform=linux/arm64 --push --pull --build-arg SMP_VERSION=$version -t phelger/phoss-smp-sql-arm64:$version -t phelger/phoss-smp-sql-arm64:latest -f Dockerfile-release-binary-sql .

# --------------- MongoDB -----------------------

docker buildx build --platform=linux/amd64 --push --pull --build-arg SMP_VERSION=$version -t phelger/phoss-smp-mongodb:$version -t phelger/phoss-smp-mongodb:latest -f Dockerfile-release-binary-mongodb .
docker buildx build --platform=linux/arm64 --push --pull --build-arg SMP_VERSION=$version -t phelger/phoss-smp-mongodb-arm64:$version -t phelger/phoss-smp-mongodb-arm64:latest -f Dockerfile-release-binary-mongodb .
