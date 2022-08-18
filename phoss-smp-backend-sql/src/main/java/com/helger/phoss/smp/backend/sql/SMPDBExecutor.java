/*
 * Copyright (C) 2019-2022 Philip Helger and contributors
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

import com.helger.commons.regex.RegExHelper;
import com.helger.commons.string.StringHelper;
import com.helger.config.IConfig;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.phoss.smp.SMPConfigSource;

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
    final String sSchemaName = SMPConfigSource.getConfig ().getAsString (SMPJDBCConfiguration.CONFIG_JDBC_SCHEMA);
    if (StringHelper.hasText (sSchemaName) && RegExHelper.stringMatchesPattern ("[0-9a-zA-Z]+", sSchemaName))
      TABLE_NAME_CUSTOMIZER = x -> sSchemaName + ".smp_" + x;
    else
      TABLE_NAME_CUSTOMIZER = x -> "smp_" + x;
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (SMPDBExecutor.class);

  public SMPDBExecutor ()
  {
    super (SMPDataSourceSingleton.getInstance ().getDataSourceProvider ());

    final IConfig aConfig = SMPConfigSource.getConfig ();

    // This is ONLY for debugging
    setDebugConnections (aConfig.getAsBoolean (SMPJDBCConfiguration.CONFIG_JDBC_DEBUG_CONNECTIONS, false));
    setDebugTransactions (aConfig.getAsBoolean (SMPJDBCConfiguration.CONFIG_JDBC_DEBUG_TRANSACTIONS, false));
    setDebugSQLStatements (aConfig.getAsBoolean (SMPJDBCConfiguration.CONFIG_JDBC_DEBUG_SQL, false));

    if (aConfig.getAsBoolean (SMPJDBCConfiguration.CONFIG_JDBC_EXECUTION_TIME_WARNING_ENABLE,
                              SMPJDBCConfiguration.DEFAULT_JDBC_EXECUTION_TIME_WARNING_ENABLE))
    {
      final long nMillis = aConfig.getAsLong (SMPJDBCConfiguration.CONFIG_JDBC_EXECUTION_TIME_WARNING_MS,
                                              DBExecutor.DEFAULT_EXECUTION_DURATION_WARN_MS);
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
