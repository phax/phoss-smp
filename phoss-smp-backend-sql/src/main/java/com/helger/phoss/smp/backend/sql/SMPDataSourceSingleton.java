/*
 * Copyright (C) 2019-2026 Philip Helger and contributors
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

import java.sql.Connection;
import java.util.EnumSet;

import org.apache.commons.dbcp2.BasicDataSource;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.annotation.style.UsedViaReflection;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.string.StringImplode;
import com.helger.db.api.EDatabaseSystemType;
import com.helger.db.api.config.IJdbcConfiguration;
import com.helger.db.api.jdbc.JDBCHelper;
import com.helger.db.jdbc.ConnectionFromDataSource;
import com.helger.db.jdbc.DataSourceProviderFromJdbcConfiguration;
import com.helger.db.jdbc.IHasConnection;
import com.helger.phoss.smp.config.SMPConfigProvider;
import com.helger.scope.IScope;
import com.helger.scope.singleton.AbstractGlobalSingleton;

/**
 * DataSource provider singleton
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class SMPDataSourceSingleton extends AbstractGlobalSingleton
{
  /**
   * The default number of seconds a single database connection validation may take.
   *
   * @since 8.3.1
   */
  public static final int DEFAULT_CONNECTION_VALIDATION_TIMEOUT_SECONDS = 1;

  private static final Logger LOGGER = LoggerFactory.getLogger (SMPDataSourceSingleton.class);

  private static final EnumSet <EDatabaseSystemType> ALLOWED_DB_TYPES = EnumSet.of (EDatabaseSystemType.DB2,
                                                                                    EDatabaseSystemType.MYSQL,
                                                                                    EDatabaseSystemType.ORACLE,
                                                                                    EDatabaseSystemType.POSTGRESQL,
                                                                                    EDatabaseSystemType.SQLSERVER);
  private static final IJdbcConfiguration JDBC_CONFIG = new SMPJdbcConfiguration (SMPConfigProvider.getConfig ());
  private static final EDatabaseSystemType DB_TYPE;

  static
  {
    final String sDBType = JDBC_CONFIG.getJdbcDatabaseType ();
    DB_TYPE = EDatabaseSystemType.getFromIDCaseInsensitiveOrNull (sDBType);
    if (DB_TYPE == null || !ALLOWED_DB_TYPES.contains (DB_TYPE))
    {
      throw new IllegalStateException ("The database type MUST be provided and MUST be one of " +
                                       StringImplode.imploder ()
                                                    .source (ALLOWED_DB_TYPES, EDatabaseSystemType::getID)
                                                    .separator (", ")
                                                    .build () +
                                       " - provided value is '" +
                                       sDBType +
                                       "'");
    }
  }

  /**
   * @return The database system determined from the configuration file. Never <code>null</code>.
   */
  @NonNull
  public static EDatabaseSystemType getDatabaseType ()
  {
    return DB_TYPE;
  }

  /**
   * @return The {@link IJdbcConfiguration} instance for the SMP SQL backend. Never
   *         <code>null</code>.
   * @since 8.1.4
   */
  @NonNull
  public static IJdbcConfiguration getJdbcConfiguration ()
  {
    return JDBC_CONFIG;
  }

  private final DataSourceProviderFromJdbcConfiguration m_aDSP = new DataSourceProviderFromJdbcConfiguration (JDBC_CONFIG);

  /**
   * @deprecated Only called via reflection
   */
  @Deprecated (forRemoval = false)
  @UsedViaReflection
  public SMPDataSourceSingleton ()
  {}

  @NonNull
  public static SMPDataSourceSingleton getInstance ()
  {
    return getGlobalSingleton (SMPDataSourceSingleton.class);
  }

  @Override
  protected void onBeforeDestroy (@NonNull final IScope aScopeToBeDestroyed) throws Exception
  {
    // Close the DataSource provider
    StreamHelper.close (m_aDSP);
  }

  /**
   * @return The singleton DataSource provider to use. Uses the configuration file to determine the
   *         settings.
   */
  @NonNull
  public DataSourceProviderFromJdbcConfiguration getDataSourceProvider ()
  {
    return m_aDSP;
  }

  /**
   * Check whether a usable connection to the configured database can currently be established.
   * Contrary to just taking a connection from the pool, the retrieved connection is explicitly
   * validated, because the connection pool hands out pooled connections without validating them,
   * unless <code>jdbc.pooling.test-on-borrow</code> is enabled - and that is disabled by default.
   * Without the validation a connection to an already dead database would be considered to be
   * usable, until the pool evicts it.
   *
   * @param nValidationTimeoutSeconds
   *        The maximum number of seconds the validation of the connection may take. Must be &ge; 0,
   *        where 0 means "no timeout".
   * @return <code>true</code> if a usable connection was established, <code>false</code> otherwise.
   * @since 8.3.1
   */
  public static boolean isDBConnectionPossible (@Nonnegative final int nValidationTimeoutSeconds)
  {
    final BasicDataSource aDS = getInstance ().getDataSourceProvider ().getDataSource ();

    // Note: maxReconnects setting for MySQL makes no difference
    final IHasConnection aCP = new ConnectionFromDataSource (aDS);
    Connection aConnection = null;
    try
    {
      // Get connection
      aConnection = aCP.getConnection ();
      if (aConnection == null)
        return false;

      // The connection may be a pooled one that was established before the database went down
      return aConnection.isValid (nValidationTimeoutSeconds);
    }
    catch (final Exception ex)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Failed to establish a usable database connection", ex);
      return false;
    }
    finally
    {
      // Close connection again (if necessary)
      JDBCHelper.close (aConnection);
    }
  }

  /**
   * Check whether a usable connection to the configured database can currently be established,
   * using {@link #DEFAULT_CONNECTION_VALIDATION_TIMEOUT_SECONDS} as the validation timeout.
   *
   * @return <code>true</code> if a usable connection was established, <code>false</code> otherwise.
   * @see #isDBConnectionPossible(int)
   * @since 8.3.1
   */
  public static boolean isDBConnectionPossible ()
  {
    return isDBConnectionPossible (DEFAULT_CONNECTION_VALIDATION_TIMEOUT_SECONDS);
  }
}
