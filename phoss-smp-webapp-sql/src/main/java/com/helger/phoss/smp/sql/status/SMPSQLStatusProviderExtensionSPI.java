/*
 * Copyright (C) 2014-2025 Philip Helger and contributors
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
package com.helger.phoss.smp.sql.status;

import java.sql.Connection;
import java.time.Duration;

import org.apache.commons.dbcp2.BasicDataSource;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.style.IsSPIImplementation;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.db.api.jdbc.JDBCHelper;
import com.helger.db.jdbc.ConnectionFromDataSource;
import com.helger.db.jdbc.IHasConnection;
import com.helger.phoss.smp.backend.sql.SMPDataSourceSingleton;
import com.helger.phoss.smp.backend.sql.SMPJDBCConfiguration;
import com.helger.phoss.smp.status.ISMPStatusProviderExtensionSPI;

/**
 * SQL specific status item provider.
 *
 * @author Philip Helger
 * @since 5.4.0
 */
@IsSPIImplementation
public class SMPSQLStatusProviderExtensionSPI implements ISMPStatusProviderExtensionSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPSQLStatusProviderExtensionSPI.class);

  private static boolean _isDBConnectionPossible ()
  {
    final BasicDataSource aDS = SMPDataSourceSingleton.getInstance ().getDataSourceProvider ().getDataSource ();

    // Note: maxReconnects setting for MySQL makes no difference
    final IHasConnection aCP = new ConnectionFromDataSource (aDS);
    Connection aConnection = null;
    try
    {
      // Get connection
      aConnection = aCP.getConnection ();
      if (aConnection == null)
        return false;

      // Okay, connection was established
      return true;
    }
    catch (final Exception ex)
    {
      return false;
    }
    finally
    {
      // Close connection again (if necessary)
      JDBCHelper.close (aConnection);
    }
  }

  @NonNull
  public ICommonsOrderedMap <String, ?> getAdditionalStatusData (final boolean bDisableLongRunningOperations)
  {
    final ICommonsOrderedMap <String, Object> ret = new CommonsLinkedHashMap <> ();
    if (SMPJDBCConfiguration.isStatusEnabled ())
    {
      // Since 5.3.0-RC5
      ret.put ("smp.sql.target-database", SMPJDBCConfiguration.getTargetDatabaseType ());

      // All smp.sql.pooling since 8.0.11
      ret.put ("smp.sql.pooling.max-connections",
               Integer.valueOf (SMPJDBCConfiguration.getJdbcPoolingMaxConnections ()));
      ret.put ("smp.sql.pooling.max-wait.duration",
               Duration.ofMillis (SMPJDBCConfiguration.getJdbcPoolingMaxWaitMillis ()).toString ());
      ret.put ("smp.sql.pooling.between-evictions-runs.duration",
               Duration.ofMillis (SMPJDBCConfiguration.getJdbcPoolingBetweenEvictionRunsMillis ()).toString ());
      ret.put ("smp.sql.pooling.min-evictable-idle",
               Duration.ofMillis (SMPJDBCConfiguration.getJdbcPoolingMinEvictableIdleMillis ()).toString ());
      ret.put ("smp.sql.pooling.remove-abandoned-timeout",
               Duration.ofMillis (SMPJDBCConfiguration.getJdbcPoolingRemoveAbandonedTimeoutMillis ()).toString ());
      // since 8.0.12
      ret.put ("smp.sql.pooling.test-on-borrow", Boolean.toString (SMPJDBCConfiguration.isJdbcPoolingTestOnBorrow ()));

      if (!bDisableLongRunningOperations)
      {
        // Since 5.4.0
        // It takes approximately 4 seconds on a local MySQL to say "no
        // connection" by default
        ret.put ("smp.sql.db.connection-possible", Boolean.valueOf (_isDBConnectionPossible ()));
      }
    }
    else
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("The listing of the specific SQL status items is disabled via the configuration");
    }
    return ret;
  }
}
