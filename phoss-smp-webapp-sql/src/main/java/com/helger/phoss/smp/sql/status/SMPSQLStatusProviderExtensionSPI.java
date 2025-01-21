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
import java.sql.SQLException;

import javax.annotation.Nonnull;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.db.api.jdbc.JDBCHelper;
import com.helger.db.jdbc.ConnectionFromDataSource;
import com.helger.db.jdbc.IHasConnection;
import com.helger.db.jdbc.executor.DBNoConnectionException;
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
    try
    {
      aDS.setLoginTimeout (1);
    }
    catch (final SQLException | UnsupportedOperationException ex)
    {
      // Not possible on BasicDataSource
    }

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
    catch (final DBNoConnectionException ex)
    {
      return false;
    }
    finally
    {
      // Close connection again (if necessary)
      if (aConnection != null && aCP.shouldCloseConnection ())
        JDBCHelper.close (aConnection);
    }
  }

  @Nonnull
  public ICommonsOrderedMap <String, ?> getAdditionalStatusData (final boolean bDisableLongRunningOperations)
  {
    final ICommonsOrderedMap <String, Object> ret = new CommonsLinkedHashMap <> ();
    if (SMPJDBCConfiguration.isStatusEnabled ())
    {
      // Since 5.3.0-RC5
      ret.put ("smp.sql.target-database", SMPJDBCConfiguration.getTargetDatabaseType ());

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
