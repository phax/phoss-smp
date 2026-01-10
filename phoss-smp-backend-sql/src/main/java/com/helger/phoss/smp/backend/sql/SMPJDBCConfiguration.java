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

import java.time.Duration;

import org.apache.commons.pool2.impl.BaseObjectPoolConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.CheckForSigned;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.misc.Since;
import com.helger.annotation.style.PresentForCodeCoverage;
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

  @Since ("8.0.11")
  private static final String CONFIG_JDBC_POOLING_MAX_CONNECTIONS = "jdbc.pooling.max-connections";
  private static final int DEFAULT_JDBC_POOLING_MAX_CONNECTIONS = GenericObjectPoolConfig.DEFAULT_MAX_TOTAL;

  @Since ("8.0.11")
  private static final String CONFIG_JDBC_POOLING_MAX_WAIT_MILLIS = "jdbc.pooling.max-wait.millis";
  private static final long DEFAULT_JDBC_POOLING_MAX_WAIT_MILLIS = true ? Duration.ofSeconds (10).toMillis ()
                                                                        : BaseObjectPoolConfig.DEFAULT_MAX_WAIT.toMillis ();

  @Since ("8.0.11")
  private static final String CONFIG_JDBC_POOLING_BETWEEN_EVICTION_RUNS_MILLIS = "jdbc.pooling.between-evictions-runs.millis";
  private static final long DEFAULT_JDBC_POOLING_BETWEEN_EVICTION_RUNS_MILLIS = BaseObjectPoolConfig.DEFAULT_DURATION_BETWEEN_EVICTION_RUNS.toMillis ();

  @Since ("8.0.11")
  private static final String CONFIG_JDBC_POOLING_MIN_EVICTABLE_IDLE_MILLIS = "jdbc.pooling.min-evictable-idle.millis";
  private static final long DEFAULT_JDBC_POOLING_MIN_EVICTABLE_IDLE_MILLIS = BaseObjectPoolConfig.DEFAULT_MIN_EVICTABLE_IDLE_DURATION.toMillis ();

  @Since ("8.0.11")
  private static final String CONFIG_JDBC_POOLING_REMOVE_ABANDONED_TIMEOUT_MILLIS = "jdbc.pooling.remove-abandoned-timeout.millis";
  // Should be AbandonedConfig.DEFAULT_REMOVE_ABANDONED_TIMEOUT_DURATION (see POOL-430)
  private static final long DEFAULT_JDBC_POOLING_REMOVE_ABANDONED_TIMEOUT_MILLIS = Duration.ofMinutes (5).toMillis ();

  @PresentForCodeCoverage
  private static final SMPJDBCConfiguration INSTANCE = new SMPJDBCConfiguration ();

  private SMPJDBCConfiguration ()
  {}

  @NonNull
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

  /**
   * @return The maximum number of active connections that can be allocated from this pool at the
   *         same time, or negative for no limit. Default is 8.
   * @since 8.0.11
   */
  @CheckForSigned
  public static int getJdbcPoolingMaxConnections ()
  {
    return _getConfig ().getAsInt (CONFIG_JDBC_POOLING_MAX_CONNECTIONS, DEFAULT_JDBC_POOLING_MAX_CONNECTIONS);
  }

  /**
   * @return The maximum number of milliseconds that the pool will wait (when there are no available
   *         connections) for a connection to be returned before throwing an exception, or -1 to
   *         wait indefinitely. Default is 10 seconds.
   * @since 8.0.11
   */
  @CheckForSigned
  public static long getJdbcPoolingMaxWaitMillis ()
  {
    return _getConfig ().getAsLong (CONFIG_JDBC_POOLING_MAX_WAIT_MILLIS, DEFAULT_JDBC_POOLING_MAX_WAIT_MILLIS);
  }

  /**
   * @return The number of milliseconds to sleep between runs of the idle object evictor thread.
   *         When non-positive, no idle object evictor thread will be run. Default is -1.
   * @since 8.0.11
   */
  @CheckForSigned
  public static long getJdbcPoolingBetweenEvictionRunsMillis ()
  {
    return _getConfig ().getAsLong (CONFIG_JDBC_POOLING_BETWEEN_EVICTION_RUNS_MILLIS,
                                    DEFAULT_JDBC_POOLING_BETWEEN_EVICTION_RUNS_MILLIS);
  }

  /**
   * @return The minimum amount of milliseconds an object may sit idle in the pool before it is
   *         eligible for eviction by the idle object evictor (if any). Default is 30 minutes.
   * @since 8.0.11
   */
  @CheckForSigned
  public static long getJdbcPoolingMinEvictableIdleMillis ()
  {
    return _getConfig ().getAsLong (CONFIG_JDBC_POOLING_MIN_EVICTABLE_IDLE_MILLIS,
                                    DEFAULT_JDBC_POOLING_MIN_EVICTABLE_IDLE_MILLIS);
  }

  /**
   * @return Timeout in milliseconds before an abandoned connection can be removed. Default is 5
   *         minutes.
   * @since 8.0.11
   */
  @CheckForSigned
  public static long getJdbcPoolingRemoveAbandonedTimeoutMillis ()
  {
    return _getConfig ().getAsLong (CONFIG_JDBC_POOLING_REMOVE_ABANDONED_TIMEOUT_MILLIS,
                                    DEFAULT_JDBC_POOLING_REMOVE_ABANDONED_TIMEOUT_MILLIS);
  }
}
