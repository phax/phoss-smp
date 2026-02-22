# Docker

```
docker volume create mssql_data
docker run -e "ACCEPT_EULA=Y" \
  -e "MSSQL_SA_PASSWORD=Sysadm;PW1234" \
  -e "MSSQL_DB=smp" \
  -e "MSSQL_USER=smp" \
  -e "MSSQL_PASSWORD=Strong;PW1234" \
  -p 1433:1433 --name mssql-dev --platform=linux/amd64 -v mssql_data:/var/opt/mssql -d mcr.microsoft.com/mssql/server:2022-latest
```

MSSQL_PASSWORD constraints:
> ERROR: Unable to set system administrator password: Password validation failed.
> The password does not meet SQL Server password policy requirements because it is not complex enough.
> The password must be at least 8 characters long and contain characters from three of the following four sets: Uppercase letters, Lowercase letters, Base 10 digits, and Symbols..
