/*
 * Copyright (C) 2019-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.smp.backend.sql;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.string.StringHelper;
import com.helger.db.jdbc.executor.DBExecutor;

/**
 * The SMP specific DB Executor
 *
 * @author Philip Helger
 * @since 5.3.1
 */
public final class SMPDBExecutor extends DBExecutor
{
  public static final Function <String, String> TABLE_NAME_CUSTOMIZER;
  static
  {
    final String sSchemaName = StringHelper.trim (SMPJDBCConfiguration.getJdbcSchema ());
    if (StringHelper.isNotEmpty (sSchemaName))
    {
      final String sDBType = SMPJDBCConfiguration.getTargetDatabaseType ();
      final EDatabaseType eDBType = EDatabaseType.getFromCaseIDInsensitiveOrNull (sDBType);

      // Quotes around are required for special chars (does not work in MySQL)
      final boolean bQuoteSchema = eDBType != EDatabaseType.MYSQL;
      if (bQuoteSchema)
        TABLE_NAME_CUSTOMIZER = x -> "\"" + sSchemaName + "\".smp_" + x;
      else
        TABLE_NAME_CUSTOMIZER = x -> sSchemaName + ".smp_" + x;
    }
    else
      TABLE_NAME_CUSTOMIZER = x -> "smp_" + x;
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (SMPDBExecutor.class);

  public SMPDBExecutor ()
  {
    super (SMPDataSourceSingleton.getInstance ().getDataSourceProvider ());

    // This is ONLY for debugging
    setDebugConnections (SMPJDBCConfiguration.isJdbcDebugConnections ());
    setDebugTransactions (SMPJDBCConfiguration.isJdbcDebugTransaction ());
    setDebugSQLStatements (SMPJDBCConfiguration.isJdbcDebugSQL ());

    if (SMPJDBCConfiguration.isJdbcExecutionTimeWarningEnabled ())
    {
      final long nMillis = SMPJDBCConfiguration.getJdbcExecutionTimeWarningMilliseconds ();
      if (nMillis > 0)
        setExecutionDurationWarnMS (nMillis);
      else
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Ignoring setting '" +
                        SMPJDBCConfiguration.CONFIG_JDBC_EXECUTION_TIME_WARNING_MS +
                        "' because it is invalid.");
    }
    else
    {
      // Zero means none
      setExecutionDurationWarnMS (0);
    }
  }
}
