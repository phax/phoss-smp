#!/bin/bash
set -e

# Start SQL Server
/opt/mssql/bin/sqlservr &
SQL_PID=$!

# wait until SQL is ready
echo "Waiting for SQL Server to start..."
# -C means "Trust server certificate (ignores validation)"
until /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -Q "SELECT 1" &>/dev/null
do
  sleep 2
done

# run init script(s)
echo "SQL Server ready, running init scripts..."

if [ -f /docker-entrypoint-initdb.d/setup.sql ]; then
  /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -d master -i /docker-entrypoint-initdb.d/setup.sql
fi

echo "Initialization complete!"
wait $SQL_PID
