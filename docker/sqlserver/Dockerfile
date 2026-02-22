FROM --platform=linux/amd64 mcr.microsoft.com/mssql/server:2022-latest

ENV ACCEPT_EULA=Y \
    MSSQL_SA_PASSWORD=Sysadm;PW1234 \
    MSSQL_DB=smp \
    MSSQL_USER=smp \
    MSSQL_PASSWORD=Strong;PW1234

USER mssql
    
COPY setup.sql /docker-entrypoint-initdb.d/
COPY entrypoint.sh /usr/local/bin/
#RUN chmod +x /usr/local/bin/entrypoint.sh

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
