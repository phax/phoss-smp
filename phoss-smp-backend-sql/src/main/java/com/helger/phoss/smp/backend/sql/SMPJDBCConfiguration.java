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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.annotation.PresentForCodeCoverage;
import com.helger.commons.annotation.Since;
import com.helger.config.IConfig;
import com.helger.db.api.config.JdbcConfiguration;
import com.helger.phoss.smp.config.SMPConfigProvider;

/**
 * Default JDBC configuration file properties
 *
 * @author Philip Helger
 */
@Immutable
public final class SMPJDBCConfiguration
{
  private static final String CONFIG_JDBC_DRIVER = "jdbc.driver";
  private static final String CONFIG_JDBC_USER = "jdbc.user";
  private static final String CONFIG_JDBC_PASSWORD = "jdbc.password";
  private static final String CONFIG_JDBC_URL = "jdbc.url";
  @Since ("5.3.0")
  private static final String CONFIG_JDBC_SCHEMA = "jdbc.schema";
  @Since ("5.3.0")
  private static final String CONFIG_JDBC_SCHEMA_CREATE = "jdbc.schema-create";
  private static final boolean DEFAULT_JDBC_SCHEMA_CREATE = false;
  private static final String CONFIG_TARGET_DATABASE = "target-database";

  @Since ("5.0.6")
  private static final String CONFIG_JDBC_EXECUTION_TIME_WARNING_ENABLED = "jdbc.execution-time-warning.enabled";
  private static final boolean DEFAULT_JDBC_EXECUTION_TIME_WARNING_ENABLED = true;

  @Since ("5.0.6")
  public static final String CONFIG_JDBC_EXECUTION_TIME_WARNING_MS = "jdbc.execution-time-warning.ms";

  // Experimental
  @Since ("5.3.0")
  private static final String CONFIG_JDBC_CACHE_SG_ENABLED = "jdbc.cache.sg.enabled";
  private static final boolean DEFAULT_JDBC_CACHE_SG_ENABLED = true;
  @Since ("5.3.0")
  private static final String CONFIG_JDBC_DEBUG_CONNECTIONS = "jdbc.debug.connections";
  private static final boolean DEFAULT_JDBC_DEBUG_CONNECTIONS = false;
  @Since ("5.3.0")
  private static final String CONFIG_JDBC_DEBUG_TRANSACTIONS = "jdbc.debug.transactions";
  private static final boolean DEFAULT_JDBC_DEBUG_TRANSACTIONS = false;
  @Since ("5.3.0")
  private static final String CONFIG_JDBC_DEBUG_SQL = "jdbc.debug.sql";
  private static final boolean DEFAULT_JDBC_DEBUG_SQL = false;

  private static final String CONFIG_SMP_STATUS_SQL_ENABLED = "smp.status.sql.enabled";
  private static final boolean DEFAULT_SMP_STATUS_SQL_ENABLED = true;

  @PresentForCodeCoverage
  private static final SMPJDBCConfiguration INSTANCE = new SMPJDBCConfiguration ();

  private SMPJDBCConfiguration ()
  {}

  @Nonnull
  private static IConfig _getConfig ()
  {
    return SMPConfigProvider.getConfig ();
  }

  @Nullable
  public static String getJdbcDriver ()
  {
    return _getConfig ().getAsString (CONFIG_JDBC_DRIVER);
  }

  @Nullable
  public static String getJdbcUser ()
  {
    return _getConfig ().getAsString (CONFIG_JDBC_USER);
  }

  @Nullable
  public static String getJdbcPassword ()
  {
    return _getConfig ().getAsString (CONFIG_JDBC_PASSWORD);
  }

  @Nullable
  public static String getJdbcUrl ()
  {
    return _getConfig ().getAsString (CONFIG_JDBC_URL);
  }

  @Nullable
  public static String getJdbcSchema ()
  {
    return _getConfig ().getAsString (CONFIG_JDBC_SCHEMA);
  }

  // TODO this should be part of the Flyway configuration
  public static boolean isJdbcSchemaCreate ()
  {
    return _getConfig ().getAsBoolean (CONFIG_JDBC_SCHEMA_CREATE, DEFAULT_JDBC_SCHEMA_CREATE);
  }

  @Nullable
  public static String getTargetDatabaseType ()
  {
    return _getConfig ().getAsString (CONFIG_TARGET_DATABASE);
  }

  public static boolean isJdbcExecutionTimeWarningEnabled ()
  {
    return _getConfig ().getAsBoolean (CONFIG_JDBC_EXECUTION_TIME_WARNING_ENABLED,
                                       DEFAULT_JDBC_EXECUTION_TIME_WARNING_ENABLED);
  }

  public static long getJdbcExecutionTimeWarningMilliseconds ()
  {
    return _getConfig ().getAsLong (CONFIG_JDBC_EXECUTION_TIME_WARNING_MS,
                                    JdbcConfiguration.DEFAULT_EXECUTION_DURATION_WARN_MS);
  }

  public static boolean isJdbcServiceGroupCacheEnabled ()
  {
    return _getConfig ().getAsBoolean (CONFIG_JDBC_CACHE_SG_ENABLED, DEFAULT_JDBC_CACHE_SG_ENABLED);
  }

  public static boolean isJdbcDebugConnections ()
  {
    return _getConfig ().getAsBoolean (CONFIG_JDBC_DEBUG_CONNECTIONS, DEFAULT_JDBC_DEBUG_CONNECTIONS);
  }

  public static boolean isJdbcDebugTransaction ()
  {
    return _getConfig ().getAsBoolean (CONFIG_JDBC_DEBUG_TRANSACTIONS, DEFAULT_JDBC_DEBUG_TRANSACTIONS);
  }

  public static boolean isJdbcDebugSQL ()
  {
    return _getConfig ().getAsBoolean (CONFIG_JDBC_DEBUG_SQL, DEFAULT_JDBC_DEBUG_SQL);
  }

  public static boolean isStatusEnabled ()
  {
    return _getConfig ().getAsBoolean (CONFIG_SMP_STATUS_SQL_ENABLED, DEFAULT_SMP_STATUS_SQL_ENABLED);
  }
}
