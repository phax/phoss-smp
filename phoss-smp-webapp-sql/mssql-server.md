# Docker

```
docker volume create mssql_data
docker run -e "ACCEPT_EULA=Y" -e "MSSQL_SA_PASSWORD=password" -p 1433:1433 --name mssql-dev --platform=linux/amd64 -v mssql_data:/var/opt/mssql -d mcr.microsoft.com/mssql/server:2022-latest
```
