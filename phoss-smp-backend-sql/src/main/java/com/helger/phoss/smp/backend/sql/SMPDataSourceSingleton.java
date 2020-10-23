/**
 * Copyright (C) 2019-2020 Philip Helger and contributors
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
import javax.annotation.concurrent.ThreadSafe;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.db.jdbc.AbstractDBConnector;
import com.helger.phoss.smp.SMPServerConfiguration;
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
  public static final class SMPDataSourceProvider extends AbstractDBConnector
  {
    private SMPDataSourceProvider ()
    {}

    @Override
    protected String getJDBCDriverClassName ()
    {
      return SMPServerConfiguration.getConfigFile ().getAsString (SMPJDBCConfiguration.CONFIG_JDBC_DRIVER);
    }

    @Override
    public String getConnectionUrl ()
    {
      return SMPServerConfiguration.getConfigFile ().getAsString (SMPJDBCConfiguration.CONFIG_JDBC_URL);
    }

    @Override
    protected String getUserName ()
    {
      return SMPServerConfiguration.getConfigFile ().getAsString (SMPJDBCConfiguration.CONFIG_JDBC_USER);
    }

    @Override
    protected String getPassword ()
    {
      return SMPServerConfiguration.getConfigFile ().getAsString (SMPJDBCConfiguration.CONFIG_JDBC_PASSWORD);
    }
  }

  private static final EDatabaseType DB_TYPE;

  static
  {
    final String sDBType = SMPServerConfiguration.getConfigFile ().getAsString (SMPJDBCConfiguration.CONFIG_TARGET_DATABASE);
    DB_TYPE = EDatabaseType.getFromIDOrNull (sDBType);
    if (DB_TYPE == null)
      throw new IllegalStateException ("The database type MUST be provided and MUST be one of " +
                                       EDatabaseType.values () +
                                       " - provided value is '" +
                                       sDBType +
                                       "'");
  }

  private final SMPDataSourceProvider m_aDSP = new SMPDataSourceProvider ();

  @Deprecated
  @UsedViaReflection
  public SMPDataSourceSingleton ()
  {}

  @Nonnull
  public static SMPDataSourceSingleton getInstance ()
  {
    return getGlobalSingleton (SMPDataSourceSingleton.class);
  }

  @Override
  protected void onBeforeDestroy (@Nonnull final IScope aScopeToBeDestroyed) throws Exception
  {
    // Close the DataSource provider
    StreamHelper.close (m_aDSP);
  }

  /**
   * @return The singleton DataSource provider to use. Uses the configuration
   *         file to determine the settings.
   */
  @Nonnull
  public SMPDataSourceProvider getDataSourceProvider ()
  {
    return m_aDSP;
  }

  /**
   * @return The database system determined from the configuration file. Never
   *         <code>null</code>.
   */
  @Nonnull
  public static EDatabaseType getDatabaseType ()
  {
    return DB_TYPE;
  }
}
