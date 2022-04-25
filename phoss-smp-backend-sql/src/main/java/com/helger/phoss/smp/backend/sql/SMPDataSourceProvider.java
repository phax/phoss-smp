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

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;

import javax.annotation.Nonnull;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.db.jdbc.IHasDataSource;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.settings.exchange.configfile.ConfigFile;

/**
 * The main data source provider, only instantiated from
 * {@link SMPDataSourceSingleton}.
 *
 * @author Philip Helger
 */
public final class SMPDataSourceProvider implements IHasDataSource, Closeable
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPDataSourceProvider.class);
  private final BasicDataSource m_aDataSource;

  SMPDataSourceProvider ()
  {
    final ConfigFile aCF = SMPServerConfiguration.getConfigFile ();

    // build data source
    // This is usually only called once on startup and than the same
    // DataSource is reused during the entire lifetime
    m_aDataSource = new BasicDataSource ();
    m_aDataSource.setDriverClassName (aCF.getAsString (SMPJDBCConfiguration.CONFIG_JDBC_DRIVER));
    final String sUserName = aCF.getAsString (SMPJDBCConfiguration.CONFIG_JDBC_USER);
    if (sUserName != null)
      m_aDataSource.setUsername (sUserName);
    final String sPassword = aCF.getAsString (SMPJDBCConfiguration.CONFIG_JDBC_PASSWORD);
    if (sPassword != null)
      m_aDataSource.setPassword (sPassword);
    m_aDataSource.setUrl (aCF.getAsString (SMPJDBCConfiguration.CONFIG_JDBC_URL));

    // settings
    m_aDataSource.setDefaultAutoCommit (Boolean.FALSE);
    m_aDataSource.setPoolPreparedStatements (true);

    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("Created new DataSource " + m_aDataSource);
  }

  @Nonnull
  public BasicDataSource getDataSource ()
  {
    return m_aDataSource;
  }

  public void close () throws IOException
  {
    try
    {
      if (m_aDataSource != null && !m_aDataSource.isClosed ())
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Now closing DataSource");

        m_aDataSource.close ();

        if (LOGGER.isInfoEnabled ())
          LOGGER.info ("Successfully closed DataSource");
      }
    }
    catch (final SQLException ex)
    {
      throw new IllegalStateException ("Failed to close DataSource " + m_aDataSource, ex);
    }
  }
}
