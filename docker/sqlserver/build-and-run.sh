#!/bin/bash

docker stop mssql-dev
docker rm mssql-dev

set -e
docker build -t my-mssql:dev .
docker run --name mssql-dev -p 1433:1433 -v mssql_data:/var/opt/mssql -d my-mssql:dev
