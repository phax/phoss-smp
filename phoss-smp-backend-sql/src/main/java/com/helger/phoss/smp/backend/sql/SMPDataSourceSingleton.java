/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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

  private static final EDatabaseType s_eDBType;

  static
  {
    final String sDBType = SMPServerConfiguration.getConfigFile ().getAsString (SMPJDBCConfiguration.CONFIG_TARGET_DATABASE);
    s_eDBType = EDatabaseType.getFromIDOrNull (sDBType);
    if (s_eDBType == null)
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
    StreamHelper.close (m_aDSP);
  }

  @Nonnull
  public SMPDataSourceProvider getDataSourceProvider ()
  {
    return m_aDSP;
  }

  @Nonnull
  public static EDatabaseType getDatabaseType ()
  {
    return s_eDBType;
  }
}
